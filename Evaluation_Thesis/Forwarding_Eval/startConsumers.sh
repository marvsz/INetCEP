#!/bin/bash
var1=$1
var2=500 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
if [ "$1" -lt "$var2" ]
then
	screen -dm bash -c "bash SendI.sh ndn2013 node/nodeA/sensor/victims/1 $(bc <<< "scale = 0; 1000000 / $var2") evaluation &> LogsPRA/$1/$2/consumer1.log"
else
	for ((c=1; c<=$var3; c++))
	do
		screen -dm bash -c "bash SendI.sh ndn2013 node/nodeA/sensor/victims/$c $(bc <<< "scale = 0; 1000000 / $var2") evaluation &> LogsPRA/$1/$2/consumer$c.log"
	done
fi
