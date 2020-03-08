#!/bin/bash

Interval=$1
while true
do
 ~/INetCEP/ccn-lite/bin/ccn-lite-peek -s ndn2013 -u 127.0.0.1/9998 "node/nodeA/sensor/victims" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f2
sleep $Interval
done
