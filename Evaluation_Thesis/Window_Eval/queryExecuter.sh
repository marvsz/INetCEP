#!/bin/bash

timeVal=$1
timeUnit=$2
hours="H"
minutes="M"
seconds="S"
millis="MS"
interval=0

if [ $timeUnit == $hours ]; then
	interval=$(bc <<< "scale = 0; 60*60* $timeVal")
fi

if [ $timeUnit == $minutes ]; then
	interval=$(bc <<< "scale = 0; 60 * $timeVal")
fi

if [ $timeUnit == $seconds ]; then
	interval=$timeVal
fi

if [ $timeUnit == $millis ]; then
	interval=$(bc <<< "scale = 3; $timeVal / 1000")
fi

while true
do
	nohup $CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  127.0.0.1/9001 -w 20 "call 8 /node/nodeA/nfn_service_Window '`date "+%H:%M:%S.%3N"`' 'pra' 'node/nodeA/sensor/victims/1' '1000' '500' 'MS'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2 >> windowLog &
sleep $interval
done
