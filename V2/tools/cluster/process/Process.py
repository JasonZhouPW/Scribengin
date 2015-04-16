from os.path import expanduser, join, abspath, dirname
from sys import path
from tabulate import tabulate
import paramiko, re, os
#Make sure the cluster package is on the path correctly
path.insert(0, dirname(dirname(abspath(__file__))))
from yarnRestApi.YarnRestApi import YarnRestApi #@UnresolvedImport

class Process(object):
  def __init__(self, role, hostname, homeDir, processIdentifier, sshKeyPath=join(expanduser("~"),".ssh/id_rsa")):
    self.role = role ;
    self.hostname = hostname;
    self.homeDir = homeDir;
    self.processIdentifier = processIdentifier;
    self.sshKeyPath = sshKeyPath
    
  def sshExecute(self, command, user = "neverwinterdp"):
    """
    SSH onto a machine, execute a command
    Returns [stdout,stderr]
    """
    key = paramiko.RSAKey.from_private_key_file(self.sshKeyPath)
    
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect( hostname = self.hostname, username = user, pkey = key )
    stdin, stdout, stderr = c.exec_command(command)
    
    stdout = stdout.read()
    stderr = stderr.read()
    c.close()
    
    return stdout,stderr
  
  def getReportDict(self):
    running = "Running"
    if not self.isRunning():
      running = "None"
      
    return  {
            "Role" : self.role,
            "Hostname": self.hostname,
            "HomeDir" : self.homeDir,
            "ProcessIdentifier" : self.processIdentifier,
            "Status" : running,
            "processID" : self.getRunningPid()
            }
  
  def getReport(self):
    report = self.getReportDict()
    return tabulate([report.values()], headers=report.keys())
    
  def report(self):
    print self.getReport()
  
  def isRunning(self):
    return len(self.getRunningPid()) > 0
  
  def getRole(self):
    return self.role
    
  def kill(self):
    self.printProgress("Killing ")
    pids = self.getRunningPid()
    for pid in pids.split(","):
      self.sshExecute("kill -9 "+pid)
    
  def getRunningPid(self):
    command = "ps ax | grep -w '"+self.processIdentifier+"' | grep java | grep -v grep | awk '{print $1}'"
    stdout,stderr = self.sshExecute(command)
    return stdout.strip().replace("\n",",")
  
  def isDataDirExists(self):
    if self.sshExecute("if [ -d \"" + join(self.homeDir, "data") + "\" ]; then echo \"true\"; fi")[0].strip() == "true":
      return True
    else:
      return False
    
  def start(self):
    pass
  
  def shutdown(self):
    pass
  
  def clean(self):
    pass
  
  def setupClusterEnv(self, paramDict = {}):
    pass
  
  def replaceString(self, pattern, replaceStr, line):
    return re.sub(pattern, replaceStr, line.rstrip())
  
  def printProgress(self, printStr):
    print printStr + self.getRole() + " on " + self.hostname
    
############

class KafkaProcess(Process):
  def __init__(self, hostname):
    Process.__init__(self, "kafka", hostname, "/opt/kafka", "Kafka")
   
  def setupClusterEnv(self, paramDict = {}):
    zkConnect = ":2181,".join(paramDict["zkList"]) + ":2181"
    brokerID = int(re.search(r'\d+', self.hostname).group())
    fileStr = ""
    for line in open(paramDict["server_config"]).readlines():
      if re.match(re.compile("broker.id=.*"), line):
        line = re.sub("broker.id=.*", "broker.id="+`brokerID`, line.rstrip()) + "\n"
      if re.match(re.compile("zookeeper.connect=.*"), line):
        line = re.sub("zookeeper.connect=.*", "zookeeper.connect="+zkConnect, line.rstrip()) + "\n"
      fileStr = fileStr + line
    return self.sshExecute("echo \"" + fileStr + "\" > " + join(self.homeDir, "config/server.properties"))
     
    
  def start(self):
    self.printProgress("Starting ")
    return self.sshExecute(join(self.homeDir, "bin/kafka-server-start.sh")+" -daemon "+ join(self.homeDir, "config/server.properties"))
        
  def shutdown(self):
    self.printProgress("Stopping ")
    return self.sshExecute( join(self.homeDir, "bin/kafka-server-stop.sh") )
  
  def clean(self):
    self.printProgress("Cleaning data of ")
    return self.sshExecute("rm -rf "+join(self.homeDir, "data")+ " && rm -rf "+join(self.homeDir, "logs"))
    
############

class ZookeeperProcess(Process):
  def __init__(self, hostname):
    Process.__init__(self, 'zookeeper',hostname, "/opt/zookeeper", 'QuorumPeerMain')
    self.zoo_cfg_path="/opt/zookeeper/conf/zoo_sample.cfg"
    
  def setupClusterEnv(self, paramDict = {}):
    myid_path = ""
    fileStr = ""
    hostID = int(re.search(r'\d+', self.hostname).group())
    for line in open(self.zoo_cfg_path).readlines():
      if re.match(re.compile("dataDir=.*"), line):
        myid_path = line.split("=")[1].replace("\n", "")
      fileStr = fileStr + line
    self.sshExecute("mkdir -p "+ myid_path +" && echo '" + `hostID` + "' > " + join(myid_path, "myid"))
    
    for zk in paramDict["zkList"]:
      zkID = int(re.search(r'\d+', zk).group())
      line = "server."+ `zkID` + "=" + zk + ":2888:3888\n"
      fileStr = fileStr + line
    return self.sshExecute("echo '" + fileStr + "' > " + join(self.homeDir, "conf/zoo.cfg"))
    
  def start(self):
    self.printProgress("Starting ")
    return self.sshExecute("ZOO_LOG4J_PROP='INFO,ROLLINGFILE' ZOO_LOG_DIR="+join(self.homeDir,"logs")+" "+join(self.homeDir, "bin/zkServer.sh")+ " start")
    
  def shutdown(self):
    self.printProgress("Stopping ")
    return self.sshExecute( join(self.homeDir,"bin/zkServer.sh")+ " stop")
  
  def clean(self):
    self.printProgress("Cleaning data of ")
    return self.sshExecute("rm -rf "+join(self.homeDir, "data")+ " && rm -rf "+join(self.homeDir, "logs")+" && rm -rf "+ join(self.homeDir, "zookeeper.out"))

############

class HadoopDaemonProcess(Process):
  def __init__(self, role, hostname, processIdentifier, hadoopDaemonScriptPath = "unknown"):
    Process.__init__(self, role, hostname, "/opt/hadoop", processIdentifier)
    self.hadoopDaemonScriptPath = hadoopDaemonScriptPath
  
  def setupClusterEnv(self, paramDict = {}):
    slaveStr = ""
    for hWorkers in paramDict["hadoopWorkers"]:
      slaveStr = slaveStr + hWorkers + "\n"
    slavesOut = self.sshExecute("echo \"" + slaveStr + "\" > " + join(self.homeDir, "etc/hadoop/slaves"))
    
    masterStr = ""
    for hWorkers in paramDict["hadoopMasters"]:
      masterStr = masterStr + hWorkers + "\n"
    mastersOut = self.sshExecute("echo \"" + masterStr + "\" > " + join(self.homeDir, "etc/hadoop/masters"))
    
    return slavesOut + mastersOut
  
  def start(self):
    self.printProgress("Starting ")
    return self.sshExecute(join(self.homeDir, self.hadoopDaemonScriptPath) + " start " + self.getRole())
    
  def shutdown(self):
    self.printProgress("Stopping ")
    return self.sshExecute(join(self.homeDir, self.hadoopDaemonScriptPath) + " stop " + self.getRole())
  
  def clean(self):
    self.printProgress("Cleaning data of ")
    return self.sshExecute("rm -rf "+ join(self.homeDir, "data") +" && rm -rf " + join(self.homeDir, "logs") +" && "+ join(self.homeDir, "bin/hdfs") + " namenode -format") 

############
class VmMasterProcess(Process):
  def __init__(self, role, hostname):
    Process.__init__(self, role, hostname, "/opt/scribengin/scribengin/bin/", "ApplicationMaster")
    
  def setupClusterEnv(self, paramDict = {}):
    pass
  
  def start(self):
    self.printProgress("Starting ")
    self.sshExecute(join(self.homeDir, "shell.sh")+" vm start")
    
  def shutdown(self):
    self.printProgress("Stopping ")
    self.sshExecute(join(self.homeDir, "shell.sh")+" vm shutdown")
  
  def clean(self):
    pass 
  
  def isRunning(self):
    yarnConnection = YarnRestApi(self.hostname)
    if yarnConnection.getRunningApplicationMasterIds():
      return True
    else:
      return False
  
    
  def kill(self):
    return self.shutdown()
    
  def getRunningPid(self):
    yarnConnection = YarnRestApi(self.hostname)
    return ",".join(yarnConnection.getRunningApplicationMasterIds())
    
  
############
class ScribenginProcess(Process):
  def __init__(self, role, hostname):
    Process.__init__(self, role, hostname, "/opt/scribengin/scribengin/bin/", "YarnContainer")
    self.hostname = hostname
    
  def setupClusterEnv(self, paramDict = {}):
    pass
  
  def start(self):
    self.printProgress("Starting ")
    self.sshExecute(join(self.homeDir, "shell.sh")+" scribengin start")
    
  def shutdown(self):
    self.printProgress("Stopping ")
    self.sshExecute(join(self.homeDir, "shell.sh")+" scribengin shutdown")
  
  def clean(self):
    pass 
  
  
  def isRunning(self):
    yarnConnection = YarnRestApi(self.hostname)
    result = False
    for appMasterId in yarnConnection.getRunningApplicationMasterIds():
      if yarnConnection.getNumContainersForApplicationMaster(appMasterId) > 1:
        result = True
    return result
    
  def kill(self):
    return self.shutdown()
    
  def getRunningPid(self):
    yarnConnection = YarnRestApi(self.hostname)
    result = []
    for appMasterId in yarnConnection.getRunningApplicationMasterIds():
      result.append(str(yarnConnection.getNumContainersForApplicationMaster(appMasterId)))
    
    if result:
      return "NumContainers: "+",".join(result)
    else:
      return "NumContainers: 0"
  