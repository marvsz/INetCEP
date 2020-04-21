This README guides the reader through the steps he or she has to take in order to reproduce the evaluation results.
The experiments are ordered as they are written in the thesis. First we compare the throughput, accuracy, precision, recall, f-score and latency of both approaches in the forwarding plane against each other. After that we describe the tests for the different window operators. This is finished by the experiments we did with the whole INetCEP System.

# Forwarding Evaluation
Here are the experiments we did for evaluating the forwarding capabilities of both approaches
## Throughput Tets
Our problem here is, that with one producer we are only capable of sending out about 500 pkt/s consistently. So in order to achieve a higher sensor sampling rate we started multiple producers concurrently.
This does also mean, that for the periodic request approach (PRA) we have to start an equal amount of concurrent consumers that request data at the same rate. We wrote scripts that automate this process for you.
### Userland
For generating the data in the userland the script ucl_userland_throughput_test_starter.sh has to be started for the UCL. For the PRA the script pra_userland_throughput_test_starter.sh has to be started. The parameters are 
1. The overall sensor sampling rate that the tester wants to achive in pkt/s
2. The Number of how many times we want to run this experiment
3. How long the experiment should run in seconds
This means executing ucl_userland_throughput_test_starter.sh 10000 30 3600 would mean an overall sampling rate of 10.000 pkt/s will be inserted into the relay, the experiment will be repeated 30 times and will run for one hour. Beware: this will in total take 30 hours.

### Kernel
For the kernel basically the same format is used but the user has to first insert the module into the kernel. Compile it and insert it via:
sudo startKernel.sh
Then the ucl_kernel_throughput_test_starter.sh and pra_kernel_throughput_test_starter.sh can be started. The parameters are the same as the ones for the userland script. We have not found a way to automate the automatic remove of the kernel module since it requires sudo rights. please only use one repetition and after that remove the module via
sudo removeKernel.sh
Getting the node log for the Kernel is also a bit tricky. You have to monitor it via 
```
sudo tail -f /var/log/syslog
```
and then copy that to somewhere you desire.

### Plotting
The scripts for the evaluation are in the python notebook ThroughputEvaluation.ipynb. They use dataframes generated from the scripts to parse the data located in the notebook ThroughputDataGenerator.ipynb. Here first the scripts to generate the data have to be run and after that the the actual plotting scripts in the ThroughputEvaluation.ipynb notebook have to be run. We include the generated data from our data frames in the folder dataFrames.

## Latency Tests
For the Latency tests we direct the reader to ~/INetCEP/publish_scripts. Here we use the publishRemotely.sh script to automate the setup of the line topology. Since we ran tests in different spaces, i.E. Kernel and Userland, we have to differentiate for both. Note that for running tests in the kernel version you need sudo rights.
1. Setup Topology in the userland:
```
bash publishRemotely.sh latencyTestsUserland
```
2. Setup Topology in the kernel:
```
bash publishRemotely.sh latencyTestsKernel
```
We additionally ran the tests with different level of content store fillings. In order to set up the topology and fill each node with packets you have to do the following:
1. Setup Topology in the userland and fill content store with 10000 packets:
```
bash publishRemotely.sh latencyTestsUserlandFill
```
2. Setup Topology in the kernel and fill content store with 10000 packets:
```
bash publishRemotely.sh latencyTestsKernelFill
```

### Plotting
The scripts for the latency evaluation are in the python notebook LatencyEvaluation.ipynb. They build on the generated data from the python notebook LatencyDataGenerator.ipynb. We include the generated data in the folder dataFrames.

# Window Operator Evaluation
Here we describe the experiments we did in order to evaluate the window operator

## Latency
Here we again use a topology with a single broker. The broker is started via the start1node.sh script. UCL vs. PRA using the nfn-scala window operator while increasing the number of tuples in the window operator:
To start a Window Operator that uses the PRA use the following query:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'WINDOW(node/nodeA/sensor/victims/1,100,pull,scala)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```
To start a Window Operator that uses the UCL use the following query:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'WINDOW(node/nodeA/sensor/victims/1,100,S,push,scala)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```
These start window operators with a window size of 100 tuples.

## Latency, Recall Precision and Latency
C-Window in the Userland vs. C-Window in the Kernel vs. NFN-Scala Window with state in ccnlite vs. NFN-Scala Window with state in NFN-Scala:
Start a Scala Window Operator using ccn lite to store the state:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'WINDOW(node/nodeA/sensor/victims/1,100,push,scala,ccnLiteState)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```

Start a Scala Window Operator using nfn scala to store the state:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'WINDOW(node/nodeA/sensor/victims/1,100,push,scala)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```

Start a ccn-lite Window Operator. To run it in the kernel start the kernel version. to run it in the userland start the userland version:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u  127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'WINDOW(node/nodeA/sensor/victims/1,100,S,push,C)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```

These start window operators with a window size of 100 tuples.

## Latency with increasing concurrent windows
To start 40 concurrent windows use the startWindows.sh script. It takes the number of concurrent windows as the first argument, and 1 for starting the ccn-lite window operator and 2 for starting the nfn-scala window operator as the second argument.

## Plotting
Not really automated. Look into the logs to copy the data for precision/recall and latency. You have to manually find the times when a tuple was emitted in the producer log and when the output from the query was sent back to the consumer in the nodes log. Nevertheless scripts for plotting are in the WindowOperatorEvaluation.ipynb notebook.

# INetCEP Evaluation
End-to-End delay where the time is spent in the different implementations (NFN-Scala Window Operator vs. C-Window Operator):
Interest Packets / Minute when using the PRA with different Queries:

Starting a query either do it in the INetCEP/publish_scripts script via
```
bash publishRemotely.sh all $QUERYNUMBER$ $PLACEMENTALGORITHM$ $WINDOWTYPE$ $TIMEOUT$
```
* instead of \$QUERYNUMBER\$ use one of the number 1-10, where the first 5 number correspond to the 5 queries mentioned in the thesis but executed with the ucl approach. Queries 6-10 correspond to the 5 querries mentioned in the thesis but executed with the pra approach.
* instead of \$PLACEMENTALGORITHM\$ use one of local or Centralized
* instead of \$WINDOWTYPE\$ use one of scala or builtin. !Builtin Option does not work with pra!
* instead of \$TIMEOUT\$ use a timeout in seconds

An example would be
```
bash publishRemotely.sh all 1 local scala 1200
```
to make it faster you can just start the topology with 
```
bash publishRemotely.sh readmeTopology
```
And then
```
bash publishRemotely.sh executeQuery 1 local scala 1200
```
That way you do not have to copy everything.
## Plotting
Not really automated. Look into logs and copy the corresponding data to generate the data frames. INetCEP_Evaluation.ipynb notebook does the plotting.