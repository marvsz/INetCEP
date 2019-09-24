# SA-CEP-ICN Project Status (for ICDCS)

This document is used to track the current development/project related work that a resource is currently working on.

## Ali Rizvi
-  Operator Updates
    - [x] Sequence (on sensors)
    - [x] Aggregation (on sensors)
    - [x] Window (on sensors)
    - [ ] Join - Code cleanup and refactoring
    - [ ] Filter - Code cleanup and refactoring

- Query Service Creation
    - [ ] Creation of a unified query service for centralized and decentralized queries

## Manisha Luthra
- Generalize the VM start up scripts and automate the start/stop process
    - [x] Write a single script for execution of the system on a topology
    - [x] Enable execution on *any* testbed (no dependency on IP addresses)
    - [x] Enable execution on GENI - an open testbed
    - [x] Write shutdown scripts 
- [ ] Evaluation of system on open dataset - Smarter Field dataset and DEBS Grand Challenge
- [ ] Enable execution of *any* kind of query
- [ ] Generalize operator tree creation to support aforementioned point

## Jonas Höchst
- Dockerize the system 
- Test the system on Core Emulator (better visualization and interact with emulation)

## Patrick Lampe 
- Generalize the scripts to enable large scale evaluation
- Support different topologies
    - [ ] move the static information on hops from nodeData to the system. Implement using breadth first? Source files affected by this all the placement services. 
