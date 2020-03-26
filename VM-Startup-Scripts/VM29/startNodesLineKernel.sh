#!/bin/bash
work_dir="$(cd "$(dirname "$0")" ; pwd -P)/../"
source "$work_dir/VMS.cfg"

#Start nodes:
#CCN-Lite-Old Version
#$CCNL_HOME/bin/ccn-nfn-relay -v trace -u 9002 -u 127.0.0.1/9002 &
$CCNL_HOME/bin/ccn-lite-relay -v trace -u 9002 -u 127.0.0.1/9002 &
sleep 0.1
echo "Relay started in BG"

#----------------------------|
#Create Faces and Link nodes:
#----------------------------|
#9002 -> 9001
	FACEID=`$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 newUDPface any ${VMS[0]} 9001 \
  	| $CCNL_HOME/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
	sleep 0.1
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /node/nodeA $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	#$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /9001 $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	sleep 0.1

#9002 -> 9003
	FACEID=`$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 newUDPface any ${VMS[2]} 9003 \
  	| $CCNL_HOME/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
	sleep 0.1
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /node/nodeC $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	#$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /9003 $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	sleep 0.1

#9002 -> 9004
	FACEID=`$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 newUDPface any ${VMS[2]} 9003 \
	  | $CCNL_HOME/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
	sleep 0.1
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /node/nodeD $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	#$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /9004 $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	sleep 0.1

#9002 -> 9005
	FACEID=`$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 newUDPface any ${VMS[2]} 9003 \
  	| $CCNL_HOME/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
	sleep 0.1
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /node/nodeE $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	#$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /9005 $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	sleep 0.1

#9002 -> 9006
	FACEID=`$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 newUDPface any ${VMS[2]} 9003 \
	  | $CCNL_HOME/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
	sleep 0.1
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /node/nodeF $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	#$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /9006 $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	sleep 0.1

#9002 -> 9007
	FACEID=`$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 newUDPface any ${VMS[2]} 9003 \
	  | $CCNL_HOME/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
	sleep 0.1
	$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /node/nodeG $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	#$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9002 prefixreg /9007 $FACEID ndn2013   | $CCNL_HOME/bin/ccn-lite-ccnb2xml
	sleep 0.1
#-------------------------|

