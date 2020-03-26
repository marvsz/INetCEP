#!/bin/bash
cd ~/INetCEP/ccn-lite/src/ccnl-lnxkernel/ccnl-lxkernel
insmod ./ccnl-lxkernel.ko e=eth0 v=trace u=9002 x=/tmp/mgmt-nfn-relay-b.sock ccnl_lxkernel
