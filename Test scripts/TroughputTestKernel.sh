#!/bin/bash
echo "Bash version ${BASH_VERSION}..."
var1=$1
var2=200 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
for ((c=1; c<=$var3; c++))
do
	screen -d -m ~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i $c -t 2 -s 5 -u 127.0.0.1/6363 -v trace
done
echo $var3
