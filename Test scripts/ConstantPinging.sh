#!/bin/bash

Interval=$1
while true
do
 ~/INetCEP/ccn-lite/bin/ccn-lite-peek -s ndn2013 -u 127.0.0.1/9998 "/nodeA/sensor/gps1" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f2
sleep $Interval
done
