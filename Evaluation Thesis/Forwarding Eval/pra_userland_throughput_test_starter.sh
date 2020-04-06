#!/bin/bash
# first arguemtn is the sensor sampling rate, second argument the number of runs and the third argument how long it should run. so 1000 30 3600 would mean to start it for a sampling rate of 1000 pkt/s, 30 Runs and for an hour each run. This will take all in all 30 hours so beware!.
for ((c=1; c<=$2; c++))
do
bash start1node_pra_userland_throughput.sh $1 $c $3
done
