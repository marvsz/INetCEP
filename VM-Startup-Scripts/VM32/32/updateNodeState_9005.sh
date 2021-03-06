#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/../../"
source "$work_dir/VMS.cfg"
ParentIP=$1
ParentPort=$2
ParentNodeName=$3
HostedSensor=$4
deployedOperators=""
FILE=DeployedOperators.txt
if [[ -z $Interval ]]
then
	#default interval
	Interval=20
fi

if [[ -z $HostedSensor ]]
then
	#default hosted sensors
	HostedSensor="None"
fi

function join_by { local IFS="$1"; shift; echo "$*"; }

declare -A arrNodes

#9005 node will start iperf on 8005:
nohup iperf -s -p 8005 -u  &

#replacing this read with a sleep of 30 (for automated scripts)
#read -p 'iperf Started - Other nodes up? [y/n] ' Identifier
echo 'Waiting for 5 seconds..'
sleep 5
echo 'Update node state is ready to begin..'
#After this sleep elapses, we can assume that the iperf services on the other nodes have already been started.

#Simulated: Starting Battery Life = 100
Battery=100

while true
do 
	
	#5 --> 2	#5 --> 6
	B9002=$(echo $(ping -c 1 ${VMS[1]} | tail -1| awk '{print $4 / 2}' | cut -d '/' -f 2) \* $(iperf -c ${VMS[1]} -p 8002 -u -t 1 | tail -1 | awk '{print $8}') |bc)
	B9006=$(echo $(ping -c 1 ${VMS[5]} | tail -1| awk '{print $4 / 2}' | cut -d '/' -f 2) \* $(iperf -c ${VMS[5]} -p 8006 -u -t 1 | tail -1 | awk '{print $8}') |bc)
	arrNodes=( ["9002"]="9002=$B9002" 
		   ["9006"]="9006=$B9006" 
		 )

	Latency=$(join_by , ${arrNodes[@]})
	#echo $Latency

	#Important to note that if power_supply BAT0 does not exist then this device is NOT a mobile device. This is true for VM's, which is why we are going to simulate lowering battery life:
	#Battery=$(cat /sys/class/power_supply/BAT0/capacity)
	#$(cat /sys/class/power_supply/BAT0/capacity)
	#$(upower -i $(upower -e | grep BAT) | grep --color=never -E percentage|xargs|cut -d' ' -f2|sed s/%//)
	
	#Simulate Battery Life:
	if (( $Battery == 0 ))
	then
		Battery=100
	fi

	time=$(echo `date "+%M"` | bc)
	if (( $time % 5 == 0 ))
	then
	    Battery=`expr $Battery - 1`
	fi
	
	DATE_WITH_TIME=`date "+%Y%m%d-%H%M%S%3N"` #%S%3N

	if [[ -f "$FILE" ]]; then
		deployedOperators=`cat DeployedOperators.txt`
	fi

	Content="$ParentPort|$HostedSensor|$Latency|$Battery|$deployedOperators"

	#echo $Content

	#Set Content at parent (so that parent can fetch later):
	echo $Content > nodeStatusContent
	$CCNL_HOME/bin/ccn-lite-mkDSC -s ndn2013 "node/$ParentNodeName/state/nodeState" -i nodeStatusContent > binaryNodeStatusContent
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/$ParentPort addContentToCache binaryNodeStatusContent
	arrNodes=()

    sleep 50
done
