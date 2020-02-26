# Building CCN-lite

## Prerequisites

CCN-lite requires OpenSSL. Use the following to install it:
* Ubuntu: `sudo apt-get install libssl-dev`
* OS X: `brew install openssl`

## Build

1.  Clone the repository:
    ```bash
    git clone https://github.com/cn-uofbasel/ccn-lite
    ```
    Or clone the ccn-lite folder from this repository.

2.  Set environment variable `$CCNL_HOME` and add the binary folder of CCN-lite to your `$PATH`:
    Default:
    ```bash
    export CCNL_HOME="`pwd`/ccn-lite"
    export PATH=$PATH:"$CCNL_HOME/bin"
    ```
    To make these variables permanent, add them to your shell's `.rc` file, e.g. `~/.bashrc`.
    

3.  Build CCN-lite using `make`:
    ```bash
    cd $CCNL_HOME/
    mkdir build
    cd build
    export USE_NFN=1
    export USE_NACK=1
    cmake ../src
    make clean all
    ```

# Building NFN-Scala

## Prerequisites

* Get the customized nfn-scala folder from the working repository.
* Place it in your preferred directory. E.g. /home/project/nfn-scala/
* Make sure that SBT compiler is available on your machine.

## Build

1.  Navigate to the path where nfn-scala folder is placed (example):
    
    ```bash
    cd /home/project/nfn-scala/
    ```

2.  This directory contains the build.sbt file. Use this file to compile your sources:

    ```bash
    sbt compile
    ```

3.  Once the nfn-scala code compiles successfully, produce a JAR file for the Compute Server using:

    ```bash
    sbt assembly
    ```

    Once your assembly file (.JAR) has been created, it will be placed in the ../nfn-scala/target/scala-2.10/ folder. Use the .jar file to start the compute server.


* This completes the build procedures for CCN-Lite and NFN-Scala.

# SACEPICN

## Prerequisites
* Copy your public key to the remote machine using `copyKeys.sh` script
```
bash copyKeys.sh
```
* Install dependencies and copy the binaries by 
```
bash publishRemotely.sh install
```
* Set environment variables using `setEnvironment.sh` script
```
bash setEnvironment.sh
```

There are two options to start SACEPICN system. Basically Option 1 automates the entire process of option 2. 
Note: Follow only one of the below options.

## Option 1

To start SACEPICN on the cluster of resources (see VMS.cfg), please follow the steps below: 

### Setup on MAKI compute resources

1. create your personalized VM configiration file `VMS.cfg` (refer VMS_MAKI1.cfg and VMS_MAKI2.cfg) for your user and IP addresses. 
2. auto generate node data for the respective machines with the following topology:
```
             (3) -- (7)
             
              |
              
      (1) -- (2) -- (5)
      
              |      |
              
             (4)    (6)
```             
using `python generate_node_info.py`. Note: this script uses VMS.cfg as input for IP address information. Ports used are 9001, 9001,..,9001+n. (n: number of nodes).

### Setup on GENI resources

1. generate the Rspec file to request resources on GENI using `python generate_rspec.py <number of nodes> <out dir>`
2. upload the rspec, select site and reserve resources. 
3. download the manifest rspec file with IP address and port information. 
4. auto generate VMS.cfg file (refer VMS_GENI.cfg) and node data using `python manifest_to_config.py <manifest.xml>`

After the above setup execute the publishRemotely.sh script that publishes the application on either of the above resources based on the VMS.cfg file. Refer `publish_scripts/publishRemotely.sh` script. Usage:

```
bash publish_scripts/publishRemotely.sh all "Placement" [Query] [QueryStorageReadPeriod] [ShutdownTimer]
```
Query is a number that determines, which query is to be executed: 
{1: Window, 2: Filter(Window), 3: Join(Filter(Window),Filter(Window), 4: Join(Predict2(Window),Predict2(Window)), 5: Filter(Join(Predict2(Window),Predict2(Window))), 6: Heatmap(Join(Window,Window))}.
QueryStorageReadPeriod is a value in seconds that determines the interval at which the Query Store is read.
ShutdownTimer is a value in Seconds that determnies after how many seconds the nodes are shut down.

## Option 2

### Node Startup
Starting up nodes requires the following:
* Startup of the ccn-lite relay
* Linking a nfn-scala compute server with that relay
* Starting the node state update service that manages the node state and the query service

1. Starting ccn-lite relay on a network node (X) requires us to execute the startup script for that specific node. Let's take nodeID 28 as our sample node for the rest of this tutorial.

	```bash
	cd INetCEP/VM-Startup-Scripts/VM28/
	./startNodes.sh
	```

Inside the VM28 folder, the startNodes script resides. This script initializes a node, sets up the required faces and adds those faces to the node. For each node in the network, the face configuration has been done to reflect the network topology presented in the report.

2. Starting the Compute Server is similar to a node. Make sure that you are in the same /VM28 folder and then execute the following script.

	```bash
	cd INetCEP/VM-Startup-Scripts/VM28/
	./startCS.sh
	```

3. Starting the network discovery and query server is handled by one script that manages all sub-system tasks. These tasks include:

* Running the query server, that manages incoming consumer queries (stored in the query store) and re-evaluates them after a pre-defined time interval.
* Updating node state on local nodes. The node state is sent to the ccn-lite node that then sends it to the compute server. The compute server than adds this node state in its local content store.

To start decentralized query processing in the network, we have to pass the Query Type (Centralized, Random, Decentralized) and the interval of re-evaluation for the query store.


    cd INetCEP/VM-Startup-Scripts/VM28/28
    ./startRemoteAny.sh QueryDecentral 20

The startRemoteAny.sh script takes the query type (QueryDecentral -> basically the placement algorithm) that the system has to manage and the interval (20) that the query service has to consider for re-evaluations.

## Query Execution

In order to carry out query execution, we can access any node in the network and issue the following query. Here, any node in the network can act as a placement coordinator. Therefore, the query can be issued from any node to any node in the network.


    $CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 10.2.1.28/9001 -w 20 "call 9 /node/nodeA/nfn_service_Placement 'Centralized' 'Centralized' '1' 'Source' 'Client1' 'FILTER(name,WINDOW(name,victims,4,S),3=M&4>30,name)' 'Region1' '16:22:00.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2


The above query issues a ccn-lite-peek interest to nodeID 28 for a decentralized query processing. The complex query is a join that works on two filters. The filters additionally evaluate the window operators on the streams after which the window data is filtered based on column schema represented by (1,2,3,4....n. Here 3=F means check whether the 3rd column contains F, if yes, select it).
The result of each operator is fed to its parent operator, which once complete, is returned back to the consumer node.

## Placement Logic
In order to understand the placement logic, we will describe the overall query execution process along with the invoked methods and their uses.

The overall query resolution process has the following steps:
* Issue a complex query (as shown above)
* The complex query is received by the ccn-lite node
* The ccn-lite node looks up the interest, performs lambda-calculus based reduction/closure operations on the interest. Once the call keyword is encountered, the ccn-lite node understands that this is a complex query that has to be send to a compute server. The ccn-lite node then looks at the node prefix passed for computation. Here /node/nodeA is the prefix. Previously, when the compute server started and linked itself with the ccn-lite node, it added this prefix to the face list of the relay. Therefore, the ccn-lite node forwards the interest to the compute server by looking up its face list.
* On receiving the interest from the ccn-lite relay, the compute server performs function resolution through the help of the abstract machine. The abstract machine tells the interest decomposer, the service to invoke (query) and the parameters.
* Once the service has been determined, it is invoked by the interest handler.
* On service invocation, the default class handler is invoked, that matches the arguments and invokes the appropriate handler method. This in usual cases is the 'processQuery' function.
* The processQuery function then sets up the required variables, stores the query in the query store and then creates an operator tree. Each method in the code-base is accompanied with relevant documentation that can be looked up for more information. Once the operator tree has been created, network state discovery is done.
* After network state has been gathered, the paths are build up for placement. Here each path, regardless of hops is looked up, and unique 1-hop...n-hop paths are made. While the paths are being created, on each hop, the adaptive weight application is carried out. Once this is done, each path object then contains the path node information, hops and path cost.
* After the paths have been created, the path that matches the operator count and has the minimum path cost is selected. In decentralized placement, if no path is found, then the query is sent to the best 1-hop neighboring node that carries out this process.
* The selected path is then sent to the ProcessPlacement function, that traverses the path and sets in the appropriate complex queries in it.
* Once the complex query has been set, the ProcessDeployment function is invoked, that executes these queries in the network. Upon gathering the evaluated results for child operators, the top most operator is then evaluated and then the result is returned back to the consumer.

## Additions to NFN-Scala for SACEPICN
The following services, classes were added to the system in order to build up SACEPICN over NFN-Scala.

* HopObject: Representation for each hop

    /src/main/java/SACEPICN/HopObject

* Map: Representation for the operator graph in both tree and stack forms

    /src/main/java/SACEPICN/Map

* Node: Representation for each network node in the system. This contains the complex query, node name, port, parent and neighbor node links, processing tags etc.

    /src/main/java/SACEPICN/Node

* NodeInfo: This is used to represent all network nodes (determined during the node discovery process), get relevant node information and access the node list for node data (name, port, latency etc)

    /src/main/java/SACEPICN/NodeInfo

* NodeMapping: This is a representation of all nodes in the network with regards to their name prefix, port, ip

    /src/main/java/SACEPICN/NodeMapping

* Operator: An enumeration for available system operators

    /src/main/java/SACEPICN/Operator

* OperatorTree: A java class that creates the operator tree by parsing the incoming complex query in the interest and creating NFN queries from those interests. Process: Parse interest, extract operator information, put operator queries in the tree/stack and return it.

    /src/main/java/SACEPICN/OperatorTree

* Paths: This is the representation for paths that contain network nodes. Each path can contain one or more network nodes

    /src/main/java/SACEPICN/Paths

* FormattedOutput: Added code to manage numeric data in the system

    /src/main/scala/myutil/FormattedOutput

* ExecuteQuery: A service to execute all types of queries remotely

    src/main/scala/nfn/service/ExecuteQuery

* Filter: A service that provides the filter operator functionality. Additional documentation is done in code.

    src/main/scala/nfn/service/Filter

* GetContent: Get network named-content for any interest

    src/main/scala/nfn/service/GetContent

* GetData: Get data hosted on a network node (data hosted on the nodes file-system). This can be used to manage remote config files etc.

    src/main/scala/nfn/service/GetData

* Join: A services that provides JOIN operator functionality.

    src/main/scala/nfn/service/Join

* NFNService: Changes made to the NFNService to manage additional logging and fixed 

    src/main/scala/nfn/service/NFNService

* NFNServer: Changes made to the HandlePacket function that was breaking on complex interests.

    src/main/scala/nfn/NFNServer

* QueryCentralFixed: Centralized placement based on fetch-based fixed weight variance in the buildpaths function

    src/main/scala/nfn/service/QueryCentralFixed

* QueryCentralLocalNS: Centralized placement based on heartbeats approach in the getNodeStatus function

    src/main/scala/nfn/service/QueryCentralLocalNS

* QueryCentralRemNS: Centralized placement based on fetch-based adaptive weight variance

    src/main/scala/nfn/service/QueryCentralRemNS

* QueryDecentral: Decentralized placement based on fetch-based adaptive weight variance

    src/main/scala/nfn/service/QueryDecentral

* QueryDecentralFixed: Decentralized placement based on fetch-based fixed weight variance

    src/main/scala/nfn/service/QueryDecentralFixed

* QueryRandom: Initial Random placement service (improved in RandomLocal and RandomRem)

    src/main/scala/nfn/service/QueryRandom

* QueryRandomLocalNS: Random placement based on hearbeat-based adaptive weight variance

    src/main/scala/nfn/service/QueryRandomLocalNS

* QueryRandomRemNS: Random placement based on fetch-based adaptive weight variance

    src/main/scala/nfn/service/QueryRandomRemNS

* SetData: Set data on a node. This data is set on node filesystem to configure system files.

    src/main/scala/nfn/service/SetData

* UpdateNodeState: Update network state information on the compute server of a local or remote node.

    src/main/scala/nfn/service/UpdateNodeState

* Window: An implementation of the window operator. Additional documentation has been provided in the class.

    src/main/scala/nfn/service/Window

* ComputeServerStarter: Updates to the compute server started to publish SACEPICN services on the ccn-lite node

    src/main/scala/runnable/production/ComputeServerStarter



## Additions to CCN-Lite
We patched ccn-lite from the last release that supported NFN (ccn-lite 2.1) and have the latest master commits of https://github.com/cn-uofbasel/ccn-lite with the exception on some commits that mainly changed the data types.
Our goal was to extend ccn-lite in order to additionally allow producer initiated communication therefore we added three new packet types:

* Datastream Packet: A Packet that can initiate communication coming from a producer. The data is then send continuously to all consumers that have a pending interest in the data.
* Add Persistent Interest Packet: A Packet that signals, that the Interest should be always satisfied if possible, much like a subscribe message in pub-sub systems.
* Remove Persistent Interest Packet: A Packet that removes a persistent interest packet much like an unsubscribe message in a pub-sub system.

With these additions we also enable producer initiated complex event processing as in the consumers add a query interest and the datastream packages initiate the computation of the complex event.

This was achieved by making several extensions to ccn-lite:

* Adding a sensor struct: A seperate struct with an own loop that constantly creates sensor readings as event tuples and sends them to a specified relay

* Adding a sensor setting struct: A struct that holds the sensor information

* Adding a sensor tuple struct: A buffer that contains the event tuples which are sent to the node they are connected to

* Creating a utility library for packet dumping to constantly format the output of the packet

* Altering the ccn-lite-fwd mechanism in order to handle datastream packets, add constant interest packets and remove constant interest packets

In Detail:

* src/ccnl-core/include/ccnl-interest.h: New struct ccnl_pendQ_s for pending query interests. Each interest can have a pending query interest that is executed whenever a data stream packet arrives that satisfies the interest packet. ccnl_query_append_pending a function that adds a pending query interest to an interest. ccnl_interest_dup a function that duplicates a given interest. Extendsion to the struct ccnl_interest_s with boolean variables isPersistent in order to indicate that the interest is persistent, isRemove in order to indicate that this interest removes other interests (is a remove persistent interest packet). A pointer to the fist pending Query of the pending queries for the interest. If null, there are no pending queries.

* src/ccnl-core/include/ccnl-os-time.h: Change struct ccnl_timerlists_s to take a new struct legacy_timer_emu which is necessary for the ccn-lite kernel version since newer Kernel versions use this for timing. Function legacy_timer_emu_func that emulates a legacy timer for newer Kernel Versions > 4.15.0.

* src/ccnl-core/include/ccnl-pkt-util.h: add functions ccnl_pkt_interest_isPersistent and ccnl_pkt_interest_isRemove in order to distinguish between the new packet types.

* src/ccnl-core/include/ccnl-pkt.h: Change struct ccnl_pktdetail_ndntlv_s to add to boolean variables isPersistent and isRemovePersistent to differentiate between these packet types.

* src/ccnl-core/include/ccnl-prefix.h: Change struct ccnl_prefix_s by defining CCNL_PREFIX_API to be the numb er 0x02 and CCNL_PREFIX_RQI to be the number 0x08.

* src/ccnl-core/include/ccnl_relay.h: Add generic function DBL_LINKED_LIST_EMPLACE_BACK to place a list item at the back of a doubly linked list and not in the front. Add generic Function DBL_LINKED_LIST_REMOVE_FIRST to remove the first element of the given doubly linked list and not the last.

* src/ccnl-core/src/ccnl-interest.c: Change ccnl_interest_new to also add the persistent and remove flag to the new interest. Add function ccnl_interest_dup, add function ccnl_query_append_pending

* src/ccnl-core/src/time.c: Change ccnl_set_timer to conform with linux kernel versions > 4.15.0

* src/ccnl-core/src/ccnl-relay.c: Change ccnl_serve_pending, ccnl_do_ageing to not remove a persistent interest.

* src/ccnl-dump/include/ccn-lite-pktdump-util.h and src/ccnl-dump/src/ccn-lite-pktdump-util.c: Created a library for packet dumps.

* src/ccnl-fwd/src/ccnl-fwd.c: Create function ccnl_content_serve_pendingQueries to serve the pending queries of an interest packet. Change function ccnl_fwd_handleContent to handle data stream packets and react accordingly by calling a pending query if it exists. Change function ccnl_handleInterest to react to a remove persistent interest packet to remove a persistent interest. Change ccnl_ndntlv_forwarder to react to the new packets.

* src/ccnl-lnxkernel/ccn-lite-lnxkernel.c: Change includes to make them work again. Implement ccnl_open_ethdev, ccnl_open_udpdev, ccnl_realloc and ccnl_strdup. Add a Krivine Abstract machine struct to the kernel relay to enable NFN in the Kernel.

* src/ccnl-nfn/src/ccnl-nfn-common.c: Implment function ccnl_nfn_local_interest_search to search for a local interest.

* src/ccnl-nfn/src/ccnl-nfn-ops.c: Implement a function str_split that splits a string with a given delimiter. Implement a function createNewPacket that creates a new data packet with a given content. This is used to store the operator state. Implement function ccnl_makeQueryPersistent which appends a nfn query to a persistent interest packet for data. This way when a data stream packet arrives and the persistent interest matches, the nfn function is executed with the newest data. Implement function window_purge_old_data that takes the previous data state, the parameters for a window and the new tuple to add and deletes the data that should not be in the new window. Implement function op_builtin_window that is a builtin window operator.

* src/ccnl-pkt/include/ccnl-pkt-builder.h and src/ccnl-pkt/src/ccnl-pkt-builder.c: Change function ccnl_mkSimpleInterest to accept the type of interest (normal, removePersistent and Persistent) and create the specific one accordingly. Implement ccnl_mkPersistentInterestObject to create a persistent interest.

* src/ccnl-pkt/include/ccnl-pkt-ndntlv.h and src/ccnl-pkt/include/ccnl-pkt-ndntlv.c: add Packet type identifier for data stream packet, persistent interest and remove persistent interest. Implement function ccnl_ndntlv_prependPersistentInterest that prepends a persistent interest packet with the according identifier.

* src/ccnl-sensor/\*: Structs and utilites that implement a sensor function.

* src/ccnl-utils/\*: Utility functions to create a new sensor, shut it down, send a persistent interest and remove a persistent interest. Furthermore to make a data stream packet.



## Services and Plots
The services that were created for the system are:

* Starting Nodes: StartNode.sh shell service (on each node)
* Starting Compute Server: StartCS.sh shell service (on each node)
* Query Service: queryService.sh shell service (on each node) to manage the query store

    /INetCEP/VM-Startup-Scripts/VM28/queryService.sh

* Update Node State: updateNodeState_NodeID.sh (on each node) shell service to gather network data from iPerf and ping tools as well as node battery status to update the nodes network state on its local Compute Server.

    /INetCEP/VM-Startup-Scripts/VM28/updateNodeState_9001.sh

* Plots: Evaluation plots for the system created in Python using matplotlib.

    /INetCEP/plots/Thesis
