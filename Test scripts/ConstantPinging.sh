#!/bin/bash
Interval=$1
count=0
while true
do
tstart=$(($(date +%s%N)/1000))
~/INetCEP/ccn-lite/bin/ccn-lite-peek -s ndn2013 -u 127.0.0.1/9001 "node/nodeA/sensor/victims" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f2
tend=$(($(date +%s%N)/1000))
tDelta=$(($tend-$tstart))
tDeltaSeconds=$(bc <<<  "scale = 3; $tDelta / 1000000")
tDeltaDelay=$(bc <<< "scale = 3; $Interval - $tDeltaSeconds")
(( count++ ))
echo "Recieved $count Packets so far."
sleep $tDeltaDelay
done
