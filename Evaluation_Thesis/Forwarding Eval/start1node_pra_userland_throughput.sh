#!/bin/bash
mkdir -p LogsPRA/$1/$2
nohup bash startNode.sh 9001 /tmp/mgmt-nfn-relay-a.sock > LogsPRA/$1/$2/node.log &
echo "started node"
sleep 1s
nohup bash startConsumers.sh $1 $2
nohup bash startProducers.sh $1 $2 PRA
sleep 1s
echo "startet everything"
sleep $3s
bash shutdown1relay.sh
