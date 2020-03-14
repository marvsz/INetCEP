#!/bin/bash
mkdir -p LogsPing/$1/$2
nohup bash startNode.sh 9001 /tmp/mgmt-nfn-relay-a.sock > LogsPing/$1/$2/node.log &
echo "started node"
sleep 1s
#nohup ~/INetCEP/ccn-lite/bin/ccn-lite-peekConstant -s ndn2013 -u 127.0.0.1/9001 "node/nodeA/sensor/victims" -v trace &> Logs/$1/$2/ConstantConsumer.log &
sleep 2s
nohup bash ThroughputTest.sh $1 $2
nohup bash ConstantPingingTest.sh $1 $2
sleep 1s
echo "startet everything"
sleep $3s
bash shutdown1relay.sh
