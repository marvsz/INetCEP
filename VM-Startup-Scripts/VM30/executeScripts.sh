#!/bin/bash
queryType=$2
queryServiceInterval=$3
executionScripts=~/INetCEP/VM-Startup-Scripts/30/

initializetopology() {
	rm *.log
	nohup bash startNodes.sh > nodes.log &
	sleep 2s
	nohup bash startCS.sh > CS.log &
	sleep 10s
}

startexecution() {
	#dirs=($(ls -d ~/INetCEP/VM-Startup-Scripts/*))
	#cd "${dirs[@]}"
	cd $executionScripts
	rm *.log
	#execute this script the last once all the nodes and compute servers are started 
	nohup bash startRemoteAny.sh $queryType $queryServiceInterval > startUp.log &
}

if [ $1 == 'initialize' ]; then initializetopology
elif [ $1 == 'start' ]; then  startexecution
fi

