# VoltDB (Online Tier) Notes: #

This document contains information and links to information about issues encountered with VoltDB, the current online tier implementation. Currently for the docker deployment model, version 9.2.1 is being used.

## Large Queries ##
* __Issue:__ When a exceptionally large query is made to VoltDB the query could fail causing DAI-DS to fail.
* __VoltDB version <  9.1:__ A query cannot exceed 50MB as a result size.
* __VoltDB version >= 9.1:__ A query cannot exceed 50% of the heap size.
* __Where Found in DAI-DS:__ During benchmarking, after inserting 1,000,0000 RAS events in a burst the AdapterRasForeignBus adapter did a query that failed in the described way.
* __Release Notes:__ [VoltDB Release Notes](https://docs.voltdb.com/ReleaseNotes/) (Section 5 for release 9.1, subsection 5.5)
* __Mitigation:__ Set the environment variable ___MP_MAX_TOTAL_RESP_SIZE___ to a percentage of the heap. Please read the linked documentation first!
