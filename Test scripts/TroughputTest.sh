#!/bin/bash
echo "Bash version ${BASH_VERSION}..."
var1=$1
var2=200 #The messages a node sensor is capable to produce at maximum
var3=$(($var1/$var2))
if [ "$var3" -eq "0" ]
then
screen -d -m ~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s $(bc <<< "scale = 0; 1000 / $var1") -x mgmt-nfn-relay-a.sock -v evaluation
else
for ((c=1; c<=$var3; c++))
do
	screen -d -m ~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i $c -t 2 -s 5 -x mgmt-nfn-relay-a.sock -v evaluation
done
echo $var3
fi


