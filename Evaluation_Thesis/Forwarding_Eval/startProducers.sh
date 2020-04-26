#!/bin/bash
var1=$1
var2=500 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
if [ "$1" -lt "$var2" ]
then
	screen -d -m bash -c "bash makeSensor.sh victims 1 2 $(bc <<< "scale = 0; 1000000 / $var2") evaluation &> Logs$3/$1/$2/producer1.log"
else
	for ((c=1; c<=$var3; c++))
	do
		screen -d -m bash -c "bash makeSensor.sh victims $c 2 $(bc <<< "scale = 0; 1000000 / $var2") evaluation &> Logs$3/$1/$2/producer$c.log"
	done
fi
