#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/.."
source "$work_dir/VMS.cfg"
count=0
declare -a VMSdir
CCNL_HOME="~/INetCEP/nfn-scala/ccn-lite-nfn" #requires project to copied at the home location (~)
all(){
copyNFN
buildNFN
moveNFN
}
copyNFN(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# make directories if they don't exist already
		ssh -t $user@$i <<-'ENDSSH'
		cd INetCEP
		rm -rf nfn-scala
		mkdir nfn-scala
		ENDSSH
		scp -rp "$work_dir"/nfn-scala/* $user@$i:~/INetCEP/nfn-scala/
	done
}
buildNFN(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# make directories if they don't exist already
		ssh -t $user@$i <<-'ENDSSH'
		cd INetCEP/nfn-scala
		sbt clean
		sbt compile
		sbt assembly
		ENDSSH
	done
}
moveNFN(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# make directories if they don't exist already
		ssh -t $user@$i <<-'ENDSSH'
		rm ~/INetCEP/computeservers/nodes/*/*.jar
		cp ~/INetCEP/nfn-scala/target/scala-2.10/*.jar ~/INetCEP/computeservers/nodes/*/
		ENDSSH
	done
}

if [ $1 == "all" ]; then all
elif [ $1 == "copyNFN()" ]; then copyNFN
elif [ $1 == "buildNFN()" ]; then buildNFN
else echo "$help"
fi
