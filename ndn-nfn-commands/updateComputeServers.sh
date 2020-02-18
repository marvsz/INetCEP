#!/bin/bash

echo "Updating Compute Servers"
cp /home/veno/Thesis/nfn-scala/target/scala-2.10/nfn-assembly-0.2.0.jar /home/veno/Thesis/computeservers/nodes/nodeA/
sleep 0.1
cp /home/veno/Thesis/nfn-scala/target/scala-2.10/nfn-assembly-0.2.0.jar /home/veno/Thesis/computeservers/nodes/nodeB/
sleep 0.1
cp /home/veno/Thesis/nfn-scala/target/scala-2.10/nfn-assembly-0.2.0.jar /home/veno/Thesis/computeservers/nodes/nodeC/
sleep 0.1
cp /home/veno/Thesis/nfn-scala/target/scala-2.10/nfn-assembly-0.2.0.jar /home/veno/Thesis/computeservers/nodes/nodeD/
sleep 0.1
cp /home/veno/Thesis/nfn-scala/target/scala-2.10/nfn-assembly-0.2.0.jar /home/veno/Thesis/computeservers/nodes/nodeE/
sleep 0.1
cp /home/veno/Thesis/nfn-scala/target/scala-2.10/nfn-assembly-0.2.0.jar /home/veno/Thesis/computeservers/nodes/nodeF/
echo "Compute Servers Updated!"
