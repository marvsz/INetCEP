#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/.."
source "$work_dir/VMS.cfg"
count=0
declare -a VMSdir
CCNL_HOME="~/INetCEP/nfn-scala/ccn-lite-nfn" #requires project to copied at the home location (~)
all(){
copyCCN
buildCCN
}
copyCCN(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# make directories if they don't exist already
		ssh -t $user@$i <<-'ENDSSH'
		cd INetCEP
		rm -rf ccn-lite
		mkdir ccn-lite
		ENDSSH
		scp -rp "$work_dir"/ccn-lite $user@$i:~/INetCEP
	done
}
buildCCN(){
for i in "${VMS[@]}"
	do
		echo "logged in: " $i 	
		# make directories if they don't exist already
		ssh -t $user@$i <<-'ENDSSH'
		cd INetCEP/ccn-lite/
		mkdir build
		cd build
		export USE_NFN=1
		export USE_NACK=1
		cmake ..
		make clean all
		ENDSSH
	done
}

if [ $1 == "all" ]; then all
elif [ $1 == "copyCCN" ]; then copyCCN
elif [ $1 == "buildCCN" ]; then buildCCN
else echo "$help"
fi
