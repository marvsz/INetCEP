#!/bin/bash

#remove face 
#$CCNL_HOME/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-g.sock debug dump | $CCNL_HOME/bin/ccn-lite-ccnb2xml

#destroy face
#$CCNL_HOME/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-g.sock destroyface $FACEID | $CCNL_HOME/bin/ccn-lite-ccnb2xml

#shutdown the relay 
$CCNL_HOME/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-g.sock debug halt | $CCNL_HOME/bin/ccn-lite-ccnb2xml
