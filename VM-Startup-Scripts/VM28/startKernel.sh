#!/bin/bash
cd ~/INetCEP/ccn-lite/src/ccnl-lnxkernel/ccnl-lxkernel
insmod ./ccnl-lxkernel.ko e=eth0 v=trace u=9001 x=/tmp/mgmt-nfn-relay-a.sock ccnl_lxkernel
