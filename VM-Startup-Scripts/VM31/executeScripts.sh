#!/bin/bash
queryType=$2
queryServiceInterval=$3
executionScripts=~/INetCEP/VM-Startup-Scripts/31/

initializetopology() {
	rm *.log
	nohup bash startNodes.sh > nodes.log &
	sleep 2s
	nohup bash startCS.sh > CS.log &
	sleep 10s
}

initializetopologyWithSensors() {
	rm *.log
	nohup bash startNodes.sh > nodes.log &
	sleep 2s
	nohup bash startSensor.sh victims 1 2 500000 trace > sensor.log &
	nohup bash startSensor.sh victims 2 2 500000 trace > sensor.log &
	sleep 1s
	nohup bash startCS.sh > CS.log &
	sleep 10s
}

initializetopologyKernel() {
	rm *.log
	nohup bash startNodes.sh > nodes.log &
	sleep 2s
	nohup bash startCS.sh > CS.log &
	sleep 10s
}

initializeTestTopologyLines() {
	rm *.log
	nohup bash startNodesLine.sh > nodes.log &
	sleep 2s
}

initializeTestTopology() {
	rm *.log
	nohup bash startNodes.sh > nodes.log &
	sleep 2s
}

initializeTestTopologyLinesKernel() {
	rm *.log
	nohup bash startNodesLineKernel.sh > nodes.log &
	sleep 2s
}

initializeTestTopologyKernel() {
	rm *.log
	nohup bash startNodesKernel.sh > nodes.log &
	sleep 2s
}

startexecution() {
	#dirs=($(ls -d ~/INetCEP/VM-Startup-Scripts/*))
	#cd "${dirs[@]}"
	cd $executionScripts
	rm *.log
	#execute this script the last once all the nodes and compute servers are started 
	nohup bash startRemoteAny.sh > startUp.log &
}

if [ $1 == 'initialize' ]; then initializetopology
elif [ $1 == 'start' ]; then  startexecution
elif [ $1 == 'initializeTest' ]; then  initializeTestTopology
elif [ $1 == 'initializeTestLine' ]; then  initializeTestTopologyLines
elif [ $1 == 'initializeTestKernel' ]; then  initializeTestTopologyKernel
elif [ $1 == 'initializeTestLineKernel' ]; then  initializeTestTopologyLinesKernel
elif [ $1 == 'initializeKernel' ]; then  initializetopologyKernel
fi

