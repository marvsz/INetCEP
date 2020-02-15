#!/bin/bash

nohup bash startNode.sh 9998 /tmp/mgmt-nfn-relay-a.sock > nodeC.log &
sleep 2s
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n gps -i 1 -t 1 -s 500 -x mgmt-nfn-relay-a.sock -v trace -d /home/johannes/INetCEP/sensors/gps1
