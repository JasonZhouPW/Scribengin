#!/bin/bash

function h1() {
  echo ""
  echo "###########################################################################################################"
  echo "$@"
  echo "###########################################################################################################"
}

function printUsage() {
  usage=`basename $0`
  usage="$usage USAGE"
  h1 $usage
  echo "./scribengin.sh build [--with-test]     Build Scribengin V2.  Optionally run tests."
  echo "./scribengin.sh deploy [--clean]        Deploy Scribengin V2 to Docker cluster.  Optionally run cluster.sh clean on all nodes."
}

function build(){
  OPTION=$1
  DOCKERSCRIBEDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  #Omit the the tests by default
  if [ "$OPTION" = "--with-test" ] ; then
    command="../gradlew clean build install release"
  else
    command="../gradlew clean build install release -x test"
  fi
  
  #Build Scribengin
  pwd=`pwd`
  h1 "Building Scribengin/V2"
  cd $DOCKERSCRIBEDIR/../../
  $command
  cd "$pwd"
  
  #Build and release the javaagent
  #cd ../../jvmagent/registry/
  #../../../gradlew clean build release -x test
  #cd "$pwd"
  
  #Release the project
  cd ../../release
  ../../gradlew clean release
  cd "$pwd"

}


function deploy(){
  OPTION=$1
  #Stop all running java processes across the cluster
  
  ssh neverwinterdp@hadoop-master -C "/opt/cluster/clusterCommander.py cluster --force-stop"
  
  if [ "$OPTION" = "--clean" ] ; then
    ssh neverwinterdp@hadoop-master -C "/opt/cluster/clusterCommander.py cluster --clean"
  fi
  
  #Copy
  ssh neverwinterdp@hadoop-master -C "/opt/cluster/clusterCommander.py cluster --execute \"rm -rf /opt/scribengin\""
  ssh neverwinterdp@hadoop-master -C "/opt/cluster/clusterCommander.py cluster --execute \"rm -rf /opt/cluster\""
  scp -r ../../release/build/release neverwinterdp@hadoop-master:/opt/scribengin
  scp -r ../../tools/cluster         neverwinterdp@hadoop-master:/opt/cluster
  ssh neverwinterdp@hadoop-master -C "/opt/cluster/clusterCommander.py cluster --sync"
}

# get command
COMMAND=$1
shift

if [ "$COMMAND" = "build" ] ; then
  build $@
elif [ "$COMMAND" = "deploy" ] ; then
  deploy $@
else
  printUsage
fi