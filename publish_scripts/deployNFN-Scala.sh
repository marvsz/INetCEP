#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/.."
source "$work_dir/VMS.cfg"
count=0
declare -a VMSdir
CCNL_HOME="~/MA-Ali/nfn-scala/ccn-lite-nfn" #requires project to copied at the home location (~)
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
		cd MA-Ali
		rm -rf nfn-scala
		mkdir nfn-scala
		ENDSSH
		scp -rp "$work_dir"/nfn-scala/* $user@$i:~/MA-Ali/nfn-scala/
	done
}
buildNFN(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# make directories if they don't exist already
		ssh -t $user@$i <<-'ENDSSH'
		cd MA-Ali/nfn-scala	
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
		rm ~/MA-Ali/computeservers/nodes/*/*.jar
		cp ~/MA-Ali/nfn-scala/target/scala-2.10/*.jar ~/MA-Ali/computeservers/nodes/*/
		ENDSSH
	done
}

if [ $1 == "all" ]; then all
elif [ $1 == "copyNFN()" ]; then copyNFN
elif [ $1 == "buildNFN()" ]; then buildNFN
else echo "$help"
fi
