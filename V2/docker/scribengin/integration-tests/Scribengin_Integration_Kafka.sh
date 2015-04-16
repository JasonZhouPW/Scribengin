#Set up docker images
DOCKERSCRIBEDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
$DOCKERSCRIBEDIR/../startCluster.sh

#make folder for test results
mkdir testresults

#Start cluster
ssh -o StrictHostKeyChecking=no neverwinterdp@hadoop-master "cd /opt/cluster && python clusterCommander.py cluster --start --clean status"

sleep 5
ssh -o "StrictHostKeyChecking no" neverwinterdp@hadoop-master "cd /opt/scribengin/scribengin && ./bin/shell.sh scribengin info"
ssh -o "StrictHostKeyChecking no" neverwinterdp@hadoop-master "cd /opt/scribengin/scribengin && ./bin/shell.sh vm info"



#Run dataflow
#ssh -o "StrictHostKeyChecking no" neverwinterdp@hadoop-master "cd /opt/scribengin/scribengin && ./bin/shell.sh  dataflow-test kafka --worker 3 --executor-per-worker 1 --duration 70000 --task-max-execute-time 1000 --source-name input --source-num-of-stream 10 --source-write-period 5 --source-max-records-per-stream 3000 --sink-name output --junit-report KafkaIntegrationTest.xml"
ssh -o "StrictHostKeyChecking no" neverwinterdp@hadoop-master "cd /opt/scribengin/scribengin && \
                              ./bin/shell.sh dataflow-test kafka-to-kakfa --dataflow-name  kafka-to-kafka \
                              --worker 3 --executor-per-worker 1 --duration 90000 --task-max-execute-time 1000 \
                              --source-name input --source-num-of-stream 10 --source-write-period 5 \
                              --source-max-records-per-stream 10000 --sink-name output  --print-dataflow-info -1 \
                              --debug-dataflow-task true --debug-dataflow-worker true \
                              --junit-report KafkaIntegrationTest.xml --dump-registry"

#Get results
scp -o stricthostkeychecking=no neverwinterdp@hadoop-master:/opt/scribengin/scribengin/KafkaIntegrationTest.xml ./testresults/

#Clean up
$DOCKERSCRIBEDIR/../stopCluster.sh

