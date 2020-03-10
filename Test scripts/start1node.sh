#!/bin/bash

nohup bash startNode.sh 9001 /tmp/mgmt-nfn-relay-a.sock > Logs/node.log &
sleep 1s
nohup bash TroughputTest.sh $1
#nohup bash ConstantPingingTest.sh $1
sleep 1s
echo "startet everything"
#~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 100 -x mgmt-nfn-relay-a.sock -v evaluation
