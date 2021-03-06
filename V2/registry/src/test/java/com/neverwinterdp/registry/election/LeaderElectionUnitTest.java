package com.neverwinterdp.registry.election;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.neverwinterdp.registry.Node;
import com.neverwinterdp.registry.NodeCreateMode;
import com.neverwinterdp.registry.Registry;
import com.neverwinterdp.registry.RegistryConfig;
import com.neverwinterdp.registry.RegistryException;
import com.neverwinterdp.registry.zk.RegistryImpl;
import com.neverwinterdp.util.FileUtil;
import com.neverwinterdp.zk.tool.server.EmbededZKServer;

public class LeaderElectionUnitTest {
  static {
    System.setProperty("log4j.configuration", "file:src/test/resources/test-log4j.properties") ;
  }
  
  final static String ELECTION_PATH = "/locks" ;
  
  private EmbededZKServer zkServerLauncher ;
  private AtomicLong lockOrder ;
  
  @Before
  public void setup() throws Exception {
    FileUtil.removeIfExist("./build/data", false);
    lockOrder = new AtomicLong() ;
    zkServerLauncher = new EmbededZKServer("./build/data/zookeeper") ;
    zkServerLauncher.start();
  }
  
  @After
  public void teardown() throws Exception {
    zkServerLauncher.shutdown();
  }

  private Registry newRegistry() {
    return new RegistryImpl(RegistryConfig.getDefault()) ;
  }
  
  @Test
  public void testElection() throws Exception {
    String DATA = "lock directory";
    Registry registry = newRegistry().connect(); 
    Node electionNode = registry.create(ELECTION_PATH, DATA.getBytes(), NodeCreateMode.PERSISTENT) ;
    registry.disconnect();
    
    Leader[] leader = new Leader[10];
    ExecutorService executorService = Executors.newFixedThreadPool(leader.length);
    for(int i = 0; i < leader.length; i++) {
      leader[i] = new Leader("worker-" + (i + 1)) ;
      executorService.execute(leader[i]);
      if(i % 10 == 0) Thread.sleep(new Random().nextInt(50));
    }
    executorService.shutdown();
    executorService.awaitTermination(3 * 60 * 1000, TimeUnit.MILLISECONDS);
  }
  
  public class Leader implements Runnable {
    String name ;
    LeaderElection election;
    
    public Leader(String name) {
      this.name = name ;
    }
    
    public void run() {
      try {
        Registry registry = newRegistry().connect();
        Node electionPath =  registry.get(ELECTION_PATH) ;
        election = electionPath.getLeaderElection();
        election.setListener(new LeaderElectionListener() {
          public void onElected() {
            System.out.println(name + " is elected");
          }
        });
        election.start();
        Node node = election.getNode();
        node.setData(name.getBytes());
        Assert.assertEquals(name, new String(node.getData())) ;
        int count = 0;
        while(count < 25) {
          try {
            Thread.sleep(500);
            if(election.isElected()) {
              election.stop();
              Thread.sleep(500);
              election.start();
            }
            count++ ;
          } catch(InterruptedException ex) {
            break;
          }
        }
        election.stop();
        registry.disconnect();
      } catch(RegistryException e) {
        e.printStackTrace();
      }
    }
  }
}
