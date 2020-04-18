#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/.."
#assuming you are at the project root
source "$work_dir/VMS.cfg"
count=0
declare -a VMSdir
# The number of the query to execute
queryType=$2
# The placement used
placementType=$3
# The approach used
approach=$4
# Thetype of window operator that should be used
executionPlace=$5
#simulation run time in seconds
simRunTime=$6
#TODO fix workaround: setting CCNL_HOME for executeQuery method (env variables of the remote machine cannot be accessed if quotes are removed from "ENDSSH" and if quotes are put then "queryType" cannot be accessed)
CCNL_HOME="~/INetCEP/ccn-lite" #requires project to copied at the home location (~) # commented for local executions

#This script automates the execution of the system remotely on seven VMS (check VMS.cfg) 
#It first sets up the environment by copying the relevant binaries and scripts for execution remotely 
#Second it starts the topology on CCN-lite emulator
#It starts the NFN compute server 
#Last it starts the query service that is the part of ICNCEP, which reads the nodeData/queryStore at regular intervals to perform continuous detection of events
#Updates node state that is utilized by the placement component
#and intializes the query on one of the VMS (in this script 28) check "nodeData/queryStore"
#finally the results appear in nodeData/queryOutput and nodeData/nodei_Log
if [[ -z $simRunTime ]]
	then
		#default runtime 10 mins
		simRunTime=600
	fi
#new Usage: bash publishRemotely.sh all 3 local ucl scala 1200
#new Usage: bash publishRemotely.sh all 3 Centralized ucl scala 1200
#new Usage: bash publishRemotely.sh all 3 local pra scala 1200
#new Usage: bash publishRemotely.sh all 3 Centralized pra scala 1200
#new Usage: bash publishRemotely.sh all 3 local ucl builtin 1200
#new Usage: bash publishRemotely.sh all 3 Centralized ucl builtin 1200
#new Usage: bash publishRemotely.sh all 3 local pra builtin 1200
#new Usage: bash publishRemotely.sh all 3 Centralized pra builtin 1200
all() {
	echo "deploying CCN"
	deployCCN
	echo "building NFN"
	buildNFN
	sleep 2s
	echo "copying Node Info"
	copyNodeInfo
	sleep 2s
	echo "copying NFN Files"
	copyNFNFiles
	sleep 2s
	echo "Deleting old logs"
	deleteOldLogs
	echo "Creating Topology"
	createTopology
	#echo "Create Line Topology Kernel"
	#createTopologyTestLineKernel
	#echo "Create Line Topology"
	#createTopologyTestLine
	sleep 2s
	echo "Starting UpdateNodestate Service"
	execute
	sleep 10s
	echo "executing Query"
	executeQueryinVMA & sleep $simRunTime; shutdown
}

readmeSetup(){
	echo "deploying CCN"
	deployCCN
	echo "building NFN"
	buildNFN
	sleep 2s
	echo "copying Node Info"
	copyNodeInfo
	sleep 2s
	echo "copying NFN Files"
	copyNFNFiles
	sleep 2s
	echo "Deleting old logs"
	deleteOldLogs
	sleep 1s
	echo "Everything is set up"
}

readmeTopology(){
	echo "Creating Topology"
	createTopology
	sleep 2s
	echo "Starting UpdateNodestate Service"
	execute
	sleep 10s
}

latencyTestsUserland(){
	echo "deploying CCN"
	deployCCN
	sleep 2s
	echo "copying Node Info"
	copyNodeInfo
	sleep 2s
	echo "Deleting old logs"
	deleteOldLogs
	echo "Create Line Topology"
	createTopologyTestLine
}

latencyTestsKernel(){
	echo "deploying CCN"
	deployCCN
	sleep 2s
	echo "copying Node Info"
	copyNodeInfo
	sleep 2s
	echo "Deleting old logs"
	deleteOldLogs
	echo "Create Line Topology Kernel"
	createTopologyTestLineKernel
}

latencyTestsUserlandFill(){
	echo "deploying CCN"
	deployCCN
	sleep 2s
	echo "copying Node Info"
	copyNodeInfo
	sleep 2s
	echo "Deleting old logs"
	deleteOldLogs
	echo "Create Line Topology"
	createTopologyTestLineFill
}

latencyTestsKernelFill(){
	echo "deploying CCN"
	deployCCN
	sleep 2s
	echo "copying Node Info"
	copyNodeInfo
	sleep 2s
	echo "Deleting old logs"
	deleteOldLogs
	echo "Create Line Topology Kernel"
	createTopologyTestLineKernelFill
}



installDependencies() {
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i<<-ENDSSH
		echo "$sudoPW" | sudo -S apt-get update
		echo "$sudoPW" | sudo -S apt-get remove scala-library scala		
		echo "$sudoPW" | sudo -S apt-get install -y build-essential libssl-dev default-jdk default-jre bc iperf
		cd ~/Download
		wget https://scala-lang.org/files/archive/scala-2.13.1.deb
		echo "$sudoPW" | sudo -S dpkg -i scala-2.13.1.deb
		wget https://dl.bintray.com/sbt/debian/sbt-1.3.8.deb
		echo "$sudoPW" | sudo -S dpkg -i sbt-1.3.8.deb
		cd ~/Download
		wget https://cmake.org/files/v3.17/cmake-3.17.0-rc1.tar.gz
		tar -xzvf cmake-3.17.0-rc1.tar.gz
		cd cmake-3.17.0-rc1/
		./bootstrap
		make -j4
		echo "$sudoPW" | sudo -S make install
		echo "$sudoPW" | sudo -S apt install doxygen -y
		cd ~/Download
		echo "Getting openssl1.1.0f"
		wget https://www.openssl.org/source/openssl-1.1.1d.tar.gz
		tar xzvf openssl-1.1.1d.tar.gz
		cd openssl-1.1.1d
		./config -Wl,--enable-new-dtags,-rpath,'$(LIBRPATH)'
		make
		echo "$sudoPW" | sudo -S make install
		cd /usr/local/lib/
		echo "$sudoPW" | sudo -S cp libcrypto.so.1.1 /usr/lib/
		echo "$sudoPW" | sudo -S cp libcrypto.a /usr/lib/
		echo "$sudoPW" | sudo -S cp libssl.so.1.1 /usr/lib/
		echo "$sudoPW" | sudo -S cp libssl.a /usr/lib/
		cd /usr/lib/
		echo "$sudoPW" | sudo -S ln -s libcrypto.so.1.1 libcrypto.so
		echo "$sudoPW" | sudo -S ln -s libssl.so.1.1 libssl.so
		echo "$sudoPW" | sudo -S ldconfig
		ENDSSH
	done
}

installGCC7(){
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i<<-ENDSSH
		echo "$sudoPW" | sudo -S update-alternatives --remove-all gcc 
		echo "$sudoPW" | sudo -S update-alternatives --remove-all g++
		echo "$sudoPW" | sudo -S apt-get install -y software-properties-common
		echo "$sudoPW" | sudo -S add-apt-repository ppa:ubuntu-toolchain-r/test
		echo "$sudoPW" | sudo -S apt update
		echo "$sudoPW" | sudo -S apt install g++-7 -y
		echo "$sudoPW" | sudo -S update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-5 10
		echo "$sudoPW" | sudo -S update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-7 20
		echo "$sudoPW" | sudo -S update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-5 10
		echo "$sudoPW" | sudo -S update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-7 20
		echo "$sudoPW" | sudo -S update-alternatives --install /usr/bin/cc cc /usr/bin/gcc 30
		echo "$sudoPW" | sudo -S update-alternatives --set cc /usr/bin/gcc
		echo "$sudoPW" | sudo -S update-alternatives --install /usr/bin/c++ c++ /usr/bin/g++ 30
		echo "$sudoPW" | sudo -S update-alternatives --set c++ /usr/bin/g++
		ENDSSH
	done
}

restartVMs(){
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh $user@$i <<-ENDSSH
		echo "$sudoPW" | sudo -S reboot
		ENDSSH
		echo "rebooted: " $i
	done
}

updateVMs(){
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh $user@$i <<-ENDSSH
		echo "$sudoPW" | sudo -S apt-get update
		echo "$sudoPW" | sudo -S apt-get upgrade -y
		echo "$sudoPW" | sudo -S apt-get autoremove -y
		echo "$sudoPW" | sudo -S reboot
		ENDSSH
		echo "updated: " $i
	done
}

copyNFNFiles(){
echo "copying NFN-jar"
	#copy only the required jar
	for i in "${VMS[@]}"
	do
		scp -rp "$work_dir"/nfn-scala/target/scala-2.13/*.jar $user@$i:~/INetCEP/computeservers/nodes/*/
	done
}

deployCCN(){
echo "copying CCN-lite source code to remote and then building it on each"
bash remoteInstallCCNNFN.sh deployCCN
}

#copy the nodeInformation
#Usage : bash publishRemotely.sh copyNodeInfo()
copyNodeInfo() {
echo "copying NodeInformation"
	((count=0))
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# empty stuff in nodeData
		ssh -t $user@$i <<-'ENDSSH'	
		rm -rf ~/INetCEP/nodeData/
		mkdir -p ~/INetCEP/nodeData/
		#rm -rf ~/INetCEP/sensors
		#mkdir -p ~/INetCEP/sensors
		rm -rf ~/INetCEP/evalData
		mkdir -p ~/INetCEP/evalData
		ENDSSH
		nodesdir=($(ls -d $work_dir/computeservers/nodes/*))
		scp -rp ${nodesdir[$count]} $user@$i:~/INetCEP/computeservers/nodes/
		scp -rp "$work_dir"/nodeData $user@$i:~/INetCEP/

		scp -rp "$work_dir/VMS.cfg" $user@$i:~/INetCEP/
		#scp -rp "$work_dir"/sensors/* $user@$i:~/INetCEP/sensors/
	
		#scp -rp "$work_dir"/evalData/* $USER@$i:~/INetCEP/evalData/

		#copy the scripts only once for this VM
		ssh $user@$i 'rm -r ~/INetCEP/VM-Startup-Scripts'
		VMSdir=($(ls -d $work_dir/VM-Startup-Scripts/*))
		
		#copy the start up scripts
		scp -r ${VMSdir[$count]} $user@$i:~/INetCEP/VM-Startup-Scripts

		#copy the shutdown scripts
		ssh $user@$i 'rm -r ~/INetCEP/VM-Shutdown-Scripts'
		VMSdir=($(ls -d $work_dir/VM-Shutdown-Scripts/*))
		scp -r ${VMSdir[$count]} $user@$i:~/INetCEP/VM-Shutdown-Scripts
		
		((count++))
	
	done
}

copyTestScripts(){
((count=0))
	for i in "${VMS[@]}"
	do
		#echo "logged in: " $i
		#ssh -t $user@$i <<-'ENDSSH'	
		#rm -rf ~/INetCEP/Test\ scripts/
		#ENDSSH
		scp -rp "$work_dir"/Test\ scripts $user@$i:~/INetCEP/
		((count++))
	
	done	
}

deleteOldLogs(){
echo "deleting old logs"
for i in "${VMS[@]}"
	do
	echo "logged in: " $i 	
	# empty stuff in nodeData
	ssh -t $user@$i <<-'ENDSSH'
	cd ~/INetCEP/nodeData
	find . -name "*_Log" -type f -delete
	ENDSSH
	done

}

#initializes the topology and starts the compute server
#Usage : bash publishRemotely.sh create
createTopology() {
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-'ENDSSH'
		cd ~/INetCEP/VM-Startup-Scripts
		#call the scripts asynchronously using screen (nohup and & didn't work) in order to repeat the same for all the VMs
		#first create topology and start the compute server in all nodes
		screen -d -m bash executeScripts.sh initialize
		ENDSSH
	done

}

createTopologyTestLine() {
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-'ENDSSH'
		cd ~/INetCEP/VM-Startup-Scripts
		#call the scripts asynchronously using screen (nohup and & didn't work) in order to repeat the same for all the VMs
		#first create topology and start the compute server in all nodes
		screen -d -m bash executeScripts.sh initializeTestLine
		ENDSSH
	done

}

createTopologyTestLineFill() {
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-'ENDSSH'
		cd ~/INetCEP/VM-Startup-Scripts
		#call the scripts asynchronously using screen (nohup and & didn't work) in order to repeat the same for all the VMs
		#first create topology and start the compute server in all nodes
		screen -d -m bash executeScripts.sh initializeTestLine
		sleep 2s
		bash createFillerContent.sh 10000
		ENDSSH
	done

}

createTopologyTestLineKernel(){
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh $user@$i <<-ENDSSH
		cd ~/INetCEP/VM-Startup-Scripts
		echo "$sudoPW" | sudo -S bash startKernel.sh
		ENDSSH
	done
sleep 1s
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-'ENDSSH'
		cd ~/INetCEP/VM-Startup-Scripts
		#call the scripts asynchronously using screen (nohup and & didn't work) in order to repeat the same for all the VMs
		#first create topology and start the compute server in all nodes
		screen -d -m bash executeScripts.sh initializeTestLineKernel
		ENDSSH
	done

}

createTopologyTestLineKernelFill(){
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh $user@$i <<-ENDSSH
		cd ~/INetCEP/VM-Startup-Scripts
		echo "$sudoPW" | sudo -S bash startKernel.sh
		ENDSSH
	done
sleep 1s
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-'ENDSSH'
		cd ~/INetCEP/VM-Startup-Scripts
		#call the scripts asynchronously using screen (nohup and & didn't work) in order to repeat the same for all the VMs
		#first create topology and start the compute server in all nodes
		screen -d -m bash executeScripts.sh initializeTestLineKernel
		sleep 2s
		screen -d -m bash createFillerContent.sh $2
		ENDSSH
	done

}

compileKernel(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-'ENDSSH'
		cd ~/INetCEP/ccn-lite/src/ccnl-lnxkernel/
		rm -rf ccnl-lxkernel
		rm -rf CMakeFiles
		rm CMakeCache.txt Makefile
		cmake .
		make
		ENDSSH
	done
}

shutdownKernel(){
read -s -p "Enter Password for sudo: " sudoPW
	for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh $user@$i <<-ENDSSH
		cd ~/INetCEP/VM-Shutdown-Scripts/
		echo "$sudoPW" | sudo -S bash removeKernel.sh
		ENDSSH
	done
}

#initiates query service and node state update for operator placement
#Usage : bash publishRemotely.sh execute "QueryCentralRemNS" 20
execute() {
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		ssh -t $user@$i <<-ENDSSH
		cd ~/INetCEP/VM-Startup-Scripts
		#second start the query service and update node state 
		bash executeScripts.sh start
		ENDSSH
	done

}

#initiates the query on the first VM
#Usage : DEPRECATED. NOT THE REAL USAGE ANYMORE bash publishRemotely.sh executeQuery "Placement" 1 (1: Centralized  and 2: Decentralized)
executeQueryinVMA() {
	ssh $user@${VMS[0]} <<-ENDSSH
	echo "logged in "${VMS[0]}
	#one of a kind query
	case $queryType in
	1)
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  ${VMS[0]}/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement '$placementType' '$approach' '1' 'Source' 'Client1' 'WINDOW($approach,node/nodeA/sensor/victims/1,4,S,$executionPlace)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	;;
	2)
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  ${VMS[0]}/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement '$placementType' '$approach' '1' 'Source' 'Client1' 'FILTER(WINDOW($approach,node/nodeA/sensor/victims/1,4,S,$executionPlace),Gender=M&Age<15)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	;;
	3)
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  ${VMS[0]}/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement '$placementType' '$approach' '1' 'Source' 'Client1' 'JOIN(FILTER(WINDOW($approach,node/nodeA/sensor/victims/1,4,S,$executionPlace),Gender=M&Age<15),FILTER(WINDOW($approach,node/nodeA/sensor/victims/2,4,S,$executionPlace),Gender=F&Age>30),time,none,inner)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	;;
	4)
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  ${VMS[0]}/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement '$placementType' '$approach' '1' 'Source' 'Client1' 'HEATMAP(0.0015,8.7262659072876,8.8215389251709,51.7832946777344,51.8207664489746,JOIN(WINDOW($approach,node/nodeA/sensor/gps/1,5,S,$executionPlace),WINDOW($approach,node/nodeA/sensor/gps/2,5,S,$executionPlace),date,none,innerjoin))' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	;;
	5)
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  ${VMS[0]}/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement '$placementType' '$approach' '1' 'Source' 'Client1' 'FILTER(JOIN(PREDICT2(30s,WINDOW($approach,node/nodeA/sensor/plug/1,5,S,$executionPlace)),PREDICT2(30s,WINDOW($approach,node/nodeA/sensor/plug/1,5,S,$executionPlace)),date,fullouter,none),Value>50)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	;;
	*) echo "do_nothing"
	;;
	esac
	ENDSSH

}

# builds the nfn-scala alongwith SACEPICN code into a single jar located in nfn-scale/target.. This is then copied using setup
# Usage bash publishRemotely.sh build
buildNFN(){
echo "building NFN-Scala"
	cd $work_dir/nfn-scala	
	sbt clean
	sbt compile
	sbt assembly
	cd ..
}

# builds CCN-lite binaries required to start the emulation 
# Usage bash publishRemotely.sh build
buildCCNLite(){
echo "building CCNLite"
	cd $work_dir/ccn-lite/src
	export USE_NFN=1
	export USE_NACK=1
	cmake .
	make clean all
	cp -r bin/ ..
}


# get the output from the machine where query was initialized (in this case VM 28)
# Usage bash publishRemotely.sh getOutput
getOutput(){
	mkdir -p $work_dir/Output2/	
	scp -r $user@${VMS[0]}:~/INetCEP/nodeData/* $work_dir/Output2/

}

# graceful shutdown of relay and face 
# Usage bash publishRemotely.sh shutdown
shutdown(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i
		# Shutdown script stops the relay and destroys the face
		ssh -t $user@$i 'bash ~/INetCEP/VM-Shutdown-Scripts/shutdown.sh'
	done
# Still the java and bash processes needs to be stopped
sleep 10s
bash $work_dir/publish_scripts/pkill.sh	
}

getLogs(){
((count=0))
VMSdir=($(ls -d $work_dir/VM-Startup-Scripts/*))
for i in "${VMS[@]}"
	do
		mkdir -p $work_dir/Logs/$count	
		scp -r $user@$i:~/INetCEP/nodeData/nodeA_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/nodeData/nodeB_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/nodeData/nodeC_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/nodeData/nodeD_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/nodeData/nodeE_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/nodeData/nodeF_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/nodeData/nodeG_Log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/VM-Startup-Scripts/CS.log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/VM-Startup-Scripts/nodes.log $work_dir/Logs/$count
		scp -r $user@$i:~/INetCEP/VM-Startup-Scripts/*/startUp.log $work_dir/Logs/$count
		((count++))
	done
}

getUnifiedTestLogs(){
mkdir -p $work_dir/UnifiedLogs
VMSdir=($(ls -d $work_dir/VM-Startup-Scripts/*))
for i in "${VMS[@]}"
	do
	scp -r $user@$i:~/INetCEP/Test\\\ scripts/Logs /media/johannes/BULK
	done
}

getPingingTestLogs(){
mkdir -p $work_dir/UnifiedLogs
VMSdir=($(ls -d $work_dir/VM-Startup-Scripts/*))
for i in "${VMS[@]}"
	do
	scp -r $user@$i:~/INetCEP/Test\\\ scripts/LogsPing /media/johannes/BULK
	done
}

help="
Invalid usage

Publish SACEPICN script

Usage: ./publishRemotely.sh <COMMAND1> <COMMAND2> <COMMAND3> <COMMAND4> <COMMAND5>

Available <COMMAND1> options:
setup: Copy the binaries and scripts to execute the application
deployccn: deploys ccn-lite to all remote machines
getlogs: pulls the logs from the remote machine
restart: reboots the VMs
update: updates the VMs
copyNodeInfo: copies the Information necessary to run the nodes
build: build the SACEPICN app
create: Creates and starts the topology on ccn-lite emulator
execute: Starts the query service, update node state and executes the placement strategy
executeQuery: Initializes the query on VM A (1st VM in VMS.cfg)
shutdown: Shutdown the emulation
getOutput: pulls the Output from the VMs
shutdown: properly shuts down the machines
all: Run all steps to publish the cluster and start the application

Available <COMMAND2> options (only with COMMAND1=all): Query
input: {N}:  Any natural number that represents a Query, {1: Window, 2: Filter(Window), 3: Join(Filter(Window),Filter(Window), 4: Filter(Join(Predict2(Window),Predict2(Window))), 5: Heatmap(Join(Window,Window))}

Available <COMMAND3> options (only with COMMAND1=all): Query Placement type
input: {local, Centralized}:  local placement or centralized placement, more are not yet adjusted.

Available <COMMAND4> options (only with COMMAND1=all): Approach
input: {ucl, pra}: Either let it use the unified communication layer or the pra.

Available <COMMAND5> options (only with COMMAND1=all): Type of Window operator
input: {scala, builtin}: Either the nfn-scala window operator or the builtin window operator.

Available <COMMAND6> options (only with COMMAND1=all): Run duration
ipnut: {N}: Any natural number that represents the duration for which the query should run.

"


if [ $1 == "all" ]; then all
elif [ $1 == "deletelogs" ]; then deleteOldLogs
elif [ $1 == "deployccn" ]; then deployCCN
elif [ $1 == "getlogs" ]; then getLogs
elif [ $1 == "restart" ]; then restartVMs
elif [ $1 == "update" ]; then updateVMs
elif [ $1 == "compileKernel" ]; then compileKernel
elif [ $1 == "copyNodeInfo" ]; then copyNodeInfo
elif [ $1 == "install" ]; then installDependencies
elif [ $1 == "build" ]; then buildNFN #&& buildCCNLite
elif [ $1 == "create" ]; then createTopology
elif [ $1 == "execute" ]; then execute
elif [ $1 == "executeQuery" ]; then executeQueryinVMA
elif [ $1 == "getOutput" ]; then getOutput
elif [ $1 == "upgradegcc" ]; then installGCC7
elif [ $1 == "copyTest" ]; then copyTestScripts
elif [ $1 == "shutdownKernel" ]; then shutdownKernel
elif [ $1 == "shutdown" ]; then shutdown
elif [ $1 == "unifiedLogs" ]; then getUnifiedTestLogs
elif [ $1 == "pingLogs" ]; then getPingingTestLogs
elif [ $1 == "lineFill" ]; then createTopologyTestLineFill
elif [ $1 == "kernelLineFill" ]; then createTopologyTestLineKernelFill
elif [ $1 == "latencyTestsUserland" ]; then latencyTestsUserland
elif [ $1 == "latencyTestsKernel" ]; then latencyTestsKernel
elif [ $1 == "latencyTestsUserlandFill" ]; then latencyTestsUserlandFill
elif [ $1 == "latencyTestsKernelFill" ]; then latencyTestsKernelFill
elif [ $1 == "readmeSetup" ]; then readmeSetup
elif [ $1 == "readmeTopology" ]; then readmeTopology
else echo "$help"
fi

