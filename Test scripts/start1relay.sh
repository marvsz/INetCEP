#!/bin/bash

nohup bash startNode.sh 9001 /tmp/mgmt-nfn-relay-a.sock > node.log &
sleep 2
#nohup bash startCS.sh a 9001 9011 > CS.log &
#sleep 6
#nohup bash ThroughputTest.sh 500000 1
#~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500 -x mgmt-nfn-relay-a.sock -v evaluation
