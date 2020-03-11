#!/bin/bash

nohup bash startNode.sh 9001 /tmp/mgmt-nfn-relay-a.sock > Logs/node.log &
sleep 1s
nohup bash ThroughputTest.sh $1
nohup bash ConstantPingingTest.sh $1
sleep 1s
echo "startet everything"
