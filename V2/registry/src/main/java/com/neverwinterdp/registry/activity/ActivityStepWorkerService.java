package com.neverwinterdp.registry.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.neverwinterdp.registry.RegistryException;

abstract public class ActivityStepWorkerService {
  private ActivityService service ;
  
  private List<ActivityStepWorker> workers = new ArrayList<>();
  private ExecutorService executorService ;
  private Random rand = new Random() ;

  @Inject
  public void onInit(Injector container) throws RegistryException {
    int numOfWorkers = 5;
    executorService = Executors.newFixedThreadPool(numOfWorkers);
    for(int i = 0; i < numOfWorkers; i++) {
      ActivityStepWorker worker = new ActivityStepWorker() ;
      workers.add(worker);
      executorService.submit(worker);
    }
    executorService.shutdown();
  
    service = new ActivityService(container, HelloActivityCoordinator.ACTIVITIES_PATH);
  }
  
  abstract public ActivityStepWorkerDescriptor getActivityStepWorkerDescriptor() ;
  
  public void exectute(Activity activity, ActivityStep step) {
    ActivityStepWorkUnit wUnit = new ActivityStepWorkUnit(activity, step) ;
    ActivityStepWorker worker = workers.get(rand.nextInt(workers.size()));
    worker.offer(wUnit);
  }
  
  public class ActivityStepWorker implements Runnable {
    private BlockingQueue<ActivityStepWorkUnit> activityStepWorkUnits = new LinkedBlockingQueue<>() ;
    
    public void offer(ActivityStepWorkUnit activityStepWorkUnit) {
      activityStepWorkUnits.add(activityStepWorkUnit);
    }
    
    @Override
    public void run() {
      ActivityStepWorkUnit activityStepWorkUnit  = null ; 
      try {
        while((activityStepWorkUnit = activityStepWorkUnits.take()) != null) {
          Activity activity = activityStepWorkUnit.getActivity() ;
          ActivityStep activityStep = activityStepWorkUnit.getActivityStep() ;
          try {
            service.updateActivityStepExecuting(activity, activityStep, getActivityStepWorkerDescriptor());
            ActivityStepExecutor executor = 
                service.getActivityStepExecutor(activityStepWorkUnit.getActivityStep().getExecutor());
            executor.execute(activityStepWorkUnit.getActivity(), activityStepWorkUnit.getActivityStep());
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            service.updateActivityStepFinished(activity, activityStep);
          }
        }
      } catch (InterruptedException e) {
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
  
  static public class ActivityStepWorkUnit {
    private Activity activity;
    private ActivityStep activityStep ;
    
    public ActivityStepWorkUnit(Activity activity, ActivityStep activityStep) {
      this.activity = activity;
      this.activityStep = activityStep;
    }
    
    public Activity getActivity() { return activity ; }
    
    public ActivityStep getActivityStep() { return activityStep ; }
  }
}
