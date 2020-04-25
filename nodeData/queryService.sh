#!/bin/bash

#This script has to be started on each node capable of handling queries to utilize the queryStore and execute the commands after an interval.
input="$HOME/INetCEP/nodeData/queryStore"

ParentPort=$1
ParentNodeName=$2
timeVal=$3
timeUnit=$4
hours="H"
minutes="M"
seconds="S"
millis="MS"
Interval=0

if [ $timeUnit == $hours ]; then
	Interval=$(bc <<< "scale = 0; 60*60* $timeVal")
fi

if [ $timeUnit == $minutes ]; then
	Interval=$(bc <<< "scale = 0; 60 * $timeVal")
fi

if [ $timeUnit == $seconds ]; then
	Interval=$timeVal
fi

if [ $timeUnit == $millis ]; then
	Interval=$(bc <<< "scale = 3; $timeVal / 1000")
fi

newRunID=0
while true
do 

#check if file exists:
if [ -f "$input" ]; then

#read the file line-by-line (each line contains a user query)
while IFS= read -r var
do
	echo `date "+%Y-%m-%d %H:%M:%S.%3N"` ': Reading Query Store' 
	ID=$(echo $var | awk '{print $1;}' )
	algorithm=$(echo $var | awk '{print $2;}' )
	communicationApproach=$(echo $var | awk '{print $3;}' )
	runID=$(echo $var | awk '{print $4;}' )
	sourceOfQuery=$(echo $var | awk '{print $5;}' )
	clientID=$(echo $var | awk '{print $6;}' )
	query=$(echo $var | awk '{print $7;}' )
	region=$(echo $var | awk '{print $8;}' )
	timestamp=`date "+%H:%M:%S.%3N"` #use current time


	if (( $newRunID == 0 ))
	then
	    newRunID=`expr $runID + 1`
	else
	    newRunID=`expr $newRunID + 1`
	fi
	
	echo "$ID $algorithm $communicationApproach $newRunID $sourceOfQuery $clientID $query $region $timestamp " 
	#Here sourceOfQuery = 'QS' signifies that this query is part of the re-computation process
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/$ParentPort -w 20 "call 9 /node/$ParentNodeName/nfn_service_PlacementServices_QueryPlacement '$algorithm' '$communicationApproach' '$newRunID' 'QS' '$clientID' '$query' '$region' '$timestamp'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2 &

done < "$input"
#input is the file that we have to read. In this case, the queryStore (that is created during query execution)

fi
sleep $Interval

echo `date "+%Y-%m-%d %H:%M:%S.%3N"` ": Query Store processed - Next update scheduled after $Interval $TimeUnit" 

done
