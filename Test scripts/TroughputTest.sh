#!/bin/bash
var1=$1
var2=40 #The messages a node sensor is capable to produce at maximum. PC:200 remote:40
var3=$(($var1/$var2))
if [ "$var3" -eq "0" ]
then
	screen -d -m -L bash /home/johannes/INetCEP/Test\ scripts/makeSensor.sh 1 2 $(bc <<< "scale = 0; 1000 / $var1") mgmt-nfn-relay-a.sock evaluation
else
for ((c=1; c<=$var3; c++))
do
	screen -d -m bash -c " bash /home/johannes/INetCEP/Test\ scripts/makeSensor.sh $c 2 25 mgmt-nfn-relay-a.sock trace &> LogsPing/producer$c.log"
done
fi
