package com.neverwinterdp.scribengin.dataflow;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.neverwinterdp.registry.Node;
import com.neverwinterdp.registry.NodeCreateMode;
import com.neverwinterdp.registry.Registry;
import com.neverwinterdp.registry.RegistryException;
import com.neverwinterdp.registry.lock.Lock;
import com.neverwinterdp.registry.lock.LockId;
import com.neverwinterdp.vm.VMDescriptor;

public class DataflowRegistry {
  
  final static public String TASKS_PATH = "tasks";
  final static public String TASKS_AVAILABLE_PATH = TASKS_PATH + "/available";
  final static public String TASKS_ASSIGNED_PATH  = TASKS_PATH + "/assigned" ;
  final static public String TASKS_FINISHED_PATH  = TASKS_PATH + "/finished";
  final static public String TASKS_LOCK_PATH      = TASKS_PATH + "/locks";
  
  final static public String MASTER_PATH  = "master";
  final static public String MASTER_LEADER_PATH  = MASTER_PATH + "/leader";
  
  final static public String WORKERS_PATH = "workers";
  
  @Inject @Named("dataflow.registry.path") 
  private String             dataflowPath;
  @Inject
  private Registry           registry;
  private Node               status;
  private Node               tasksAvailable;
  private Node               tasksAssigned;
  private Node               tasksFinished;
  private Node               tasksLock;
  private Node               workers;

  public DataflowRegistry() { }
  
  public String getDataflowPath() { return this.dataflowPath ; }
  
  public String getTasksFinishedPath() { return tasksFinished.getPath() ;}
  
  public Registry getRegistry() { return this.registry ; }
  
  public DataflowDescriptor getDataflowDescriptor() throws RegistryException {
    return registry.getDataAs(dataflowPath, DataflowDescriptor.class);
  }
  
  @Inject
  public void onInit() throws Exception {
    status         = registry.createIfNotExist(dataflowPath + "/status");
    tasksAvailable = registry.createIfNotExist(dataflowPath + "/" + TASKS_AVAILABLE_PATH);
    tasksAssigned  = registry.createIfNotExist(dataflowPath + "/" + TASKS_ASSIGNED_PATH);
    tasksFinished  = registry.createIfNotExist(dataflowPath + "/" + TASKS_FINISHED_PATH);
    tasksLock      = registry.createIfNotExist(dataflowPath + "/" + TASKS_LOCK_PATH);
    
    registry.createIfNotExist(dataflowPath + "/" + MASTER_LEADER_PATH);
    
    workers = registry.createIfNotExist(dataflowPath + "/" + WORKERS_PATH);
  }
  
  public void addAvailable(DataflowTaskDescriptor taskDescriptor) throws RegistryException {
    Node node = tasksAvailable.createChild("task-", NodeCreateMode.PERSISTENT_SEQUENTIAL);
    taskDescriptor.setStoredPath(node.getPath());
    node.setData(taskDescriptor);
  }
  
  public void addWorker(VMDescriptor vmDescriptor) throws RegistryException {
    workers.createChild(vmDescriptor.getId(), vmDescriptor, NodeCreateMode.PERSISTENT);
  }
  
  public void setStatus(DataflowLifecycleStatus event) throws RegistryException {
    status.setData(event);
  }
  
  public DataflowTaskDescriptor getAssignedDataflowTaskDescriptor() throws RegistryException  {
    Lock lock = tasksLock.getLock("write") ;
    try {
      LockId lockId = lock.lock(30000);
      List<String> childrenName = tasksAvailable.getChildren();
      if(childrenName.size() == 0) return null;
      Collections.sort(childrenName);
      String childName = childrenName.get(0);
      Node childNode = tasksAvailable.getChild(childName);
      DataflowTaskDescriptor descriptor = childNode.getData(DataflowTaskDescriptor.class);
      String storedPath = tasksAssigned.getPath() + "/" + childName;
      descriptor.setStoredPath(storedPath);
      tasksAssigned.createChild(childName, descriptor, NodeCreateMode.PERSISTENT);
      childNode.delete();
      return descriptor;
    } catch(RegistryException ex) {
      throw ex;
    } finally {
      try {
        lock.unlock();
      } catch (RegistryException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  public void commitFinishedDataflowTaskDescriptor(DataflowTaskDescriptor descriptor) throws RegistryException {
    Lock lock = tasksLock.getLock("write") ;
    try {
      LockId lockId = lock.lock(10000);
      String oldStoredPath = descriptor.getStoredPath();
      String storedName = descriptor.storedName();
      String storedPath = tasksFinished.getPath() + "/" + storedName;
      descriptor.setStoredPath(storedPath) ;
      tasksFinished.createChild(storedName, descriptor, NodeCreateMode.PERSISTENT);
      registry.rdelete(oldStoredPath);
    } catch(RegistryException ex) {
      throw ex;
    } finally {
      try {
        lock.unlock();
      } catch (RegistryException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
