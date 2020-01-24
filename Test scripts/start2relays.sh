#!/bin/bash

nohup bash startNode.sh 9998 /tmp/mgmt-nfn-relay-a.sock > nodeA.log &
sleep 0.1
nohup bash startNode.sh 9999 /tmp/mgmt-nfn-relay-b.sock > nodeB.log &
sleep 2s

FACEID=`~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock newUDPface any 127.0.0.1 9999 \
  | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
sleep 0.1
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock prefixreg /nodeB $FACEID ndn2013 \
  | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
sleep 0.1
FACEID=`~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-b.sock newUDPface any 127.0.0.1 9998 \
  | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
sleep 0.1
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-b.sock prefixreg /nodeA $FACEID ndn2013 \
  | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
sleep 1s
nohup bash startCS.sh a 9998 9002 > CSA.log &
sleep 0.1
nohup bash startCS.sh a 9999 9003 > CSB.log &
sleep 6s
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n gps -i 1 -t 1 -s 500 -x mgmt-nfn-relay-a.sock -v trace -d /home/johannes/INetCEP/sensors/gps1
