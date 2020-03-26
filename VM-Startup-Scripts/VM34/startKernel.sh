#!/bin/bash
cd ~/INetCEP/ccn-lite/src/ccnl-lnxkernel/ccnl-lxkernel
insmod ./ccnl-lxkernel.ko e=eth0 v=trace u=9007 x=/tmp/mgmt-nfn-relay-g.sock ccnl_lxkernel
