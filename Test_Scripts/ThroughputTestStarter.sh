#!/bin/bash
for ((c=1; c<=$2; c++))
do
bash start1node.sh $1 $c $3
done
