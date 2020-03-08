#!/bin/bash

nohup bash startNode.sh 9998 /tmp/mgmt-nfn-relay-a.sock > nodeC.log &
sleep 2s
#~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 100 -x mgmt-nfn-relay-a.sock -v evaluation
