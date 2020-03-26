#!/bin/bash

#remove face 
#$CCNL_HOME/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock debug dump | $CCNL_HOME/bin/ccn-lite-ccnb2xml

#destroy face
#$CCNL_HOME/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock destroyface $FACEID | $CCNL_HOME/bin/ccn-lite-ccnb2xml

#shutdown the relay 
$CCNL_HOME/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock debug halt | $CCNL_HOME/bin/ccn-lite-ccnb2xml
