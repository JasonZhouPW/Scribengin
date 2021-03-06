package com.neverwinterdp.command.server;

import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.neverwinterdp.jetty.JettyServer;
import com.neverwinterdp.registry.NodeCreateMode;
import com.neverwinterdp.registry.Registry;
import com.neverwinterdp.registry.RegistryException;

public class CommandProxyServerUnitTest extends CommandProxyServletUnitTest{
  static CommandProxyServer cps;
  
  @BeforeClass
  public static void setup() throws Exception{
    CommandServerTestBase.setup();
    
    
    Registry registry = CommandServerTestBase.getNewRegistry();
    try {
      registry.connect();
    } catch (RegistryException e) {
      e.printStackTrace();
    }
    
    registry.create("/vm/commandServer", ("http://localhost:"+Integer.toString(commandPort)).getBytes(), NodeCreateMode.PERSISTENT);
    
    //Point our context to our web.xml we want to use for testing
    WebAppContext commandApp = new WebAppContext();
    commandApp.setResourceBase(CommandServerTestBase.getCommandServerFolder());
    commandApp.setDescriptor(  CommandServerTestBase.getCommandServerXml());
    
    
    //Point our context to our web.xml we want to use for testing
    WebAppContext proxyApp = new WebAppContext();
    proxyApp.setResourceBase(CommandServerTestBase.getProxyServerFolder());
    proxyApp.setDescriptor(CommandServerTestBase.getProxyServerXml());
    
    //Bring up proxy
    cps = new CommandProxyServer(proxyPort);
    cps.setHandler(proxyApp);
    cps.startServer();
    
    //Bring up commandServer using that context
    commandServer = new JettyServer(commandPort, CommandServlet.class);
    commandServer.setHandler(commandApp);
    commandServer.start();
  }
  
  @AfterClass
  public static void teardown() throws Exception{
    cps.stop();
    commandServer.stop();
    CommandServerTestBase.teardown();
  }
}
