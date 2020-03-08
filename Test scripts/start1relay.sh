#!/bin/bash

nohup bash startNode.sh 9998 /tmp/mgmt-nfn-relay-a.sock > node.log &
sleep 2
nohup bash startCS.sh a 9998 9002 > CS.log &
sleep 6
#~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500 -x mgmt-nfn-relay-a.sock -v evaluation
