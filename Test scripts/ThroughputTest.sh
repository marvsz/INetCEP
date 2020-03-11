#!/bin/bash
echo "Bash version ${BASH_VERSION}..."
var1=$1
var2=200 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
for ((c=1; c<=$var3; c++))
do
	screen -d -m bash -c "bash /home/johannes/INetCEP/Test\ scripts/makeSensor.sh $c 2 $(bc <<< "scale = 0; 1000 / $var2") mgmt-nfn-relay-a.sock trace &> Logs/producer$c.log"
done
echo $var3
