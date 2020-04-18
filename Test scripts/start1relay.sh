#!/bin/bash

nohup bash startNode.sh 9001 /tmp/mgmt-nfn-relay-a.sock > node.log &
sleep 2
#nohup bash startCS.sh a 9001 9011 > CS.log &
#sleep 1
#nohup bash makeSensor.sh victims 1 2 500000 127.0.0.1/9001 trace &
#nohup bash makeSensor.sh victims 2 2 500000 127.0.0.1/9001 trace &
#nohup bash makeSensorFromFile.sh gps 1 1 500000 127.0.0.1/9001 /home/johannes/INetCEP/sensors/gps1 trace &
#nohup bash makeSensorFromFile.sh gps 2 1 500000 127.0.0.1/9001 /home/johannes/INetCEP/sensors/gps1 trace &
#nohup bash makeSensorFromFile.sh plug 1 1 500000 127.0.0.1/9001 /home/johannes/INetCEP/sensors/plug1 trace &
#nohup bash makeSensorFromFile.sh plug 2 1 500000 127.0.0.1/9001 /home/johannes/INetCEP/sensors/plug1 trace &
#sleep 1
#nohup bash ThroughputTest.sh 500000 1
#~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500 -x mgmt-nfn-relay-a.sock -v evaluation
