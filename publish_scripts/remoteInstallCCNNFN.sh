#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/.."
#assuming you are at the project root
source "$work_dir/VMS.cfg"
count=0
declare -a VMSdir
CCNL_HOME="~/MA-Ali/nfn-scala/ccn-lite-nfn" #requires project to copied at the home location (~)
all(){
deployCCN
deployNFN
}
deployCCN(){
	bash deployCCNLite.sh all
}
deployNFN(){
	bash deployNFN-Scala.sh all
}

if [ $1 == "all" ]; then all
elif [ $1 == "deployCCN" ]; then deployCCN
elif [ $1 == "deployNFN" ]; then deployNFN
else echo "$help"
fi
