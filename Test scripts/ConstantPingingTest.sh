#!/bin/bash
echo "Bash version ${BASH_VERSION}..."
var1=$1
var2=40 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
for ((c=1; c<=$var3; c++))
do
	screen -dm bash -c "bash /home/johannes/INetCEP/Test\ scripts/ConstantPinging.sh 40 &> Logs/consumer$c.log"
done
echo $var3
