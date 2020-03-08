#!/bin/bash
echo "Bash version ${BASH_VERSION}..."
var1=$1
var2=137 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
for ((c=1; c<=$var3; c++))
do
	screen -d -m bash /home/johannes/INetCEP/Test\ scripts/ConstantPingingKernel.sh 0.001
done
echo $var3
