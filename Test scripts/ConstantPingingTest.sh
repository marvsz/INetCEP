#!/bin/bash
echo "Bash version ${BASH_VERSION}..."
var1=$1
var2=500 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
for ((c=1; c<=$var3; c++))
do
	screen -dm bash -c "bash /home/johannes/INetCEP/Test\ scripts/SendI.sh ndn2013 node/nodeA/sensor/victims $(bc <<< "scale = 0; 1000 / $var2") trace &> Logs/$1/$2/consumer$c.log"
done
echo $var3
