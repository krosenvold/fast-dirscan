Fast-Dirscan
===========

Based on the apache ant directory scanner as well as the codehaus plexus-utils fork (used
by maven), this code aims to experiment with various concurrent implementations that all intend
to achieve a high degree of mechanical sympathy.

Achieving mechanical sympathy in this area is believed to consist of the following optimizations:

A) Keeping IO subsystem optimally busy; avoiding delays caused by processing
   operations interleaved with IO
B) Avoiding repeated IO at all costs, create an API that will facilitate this in layered architectures.
C) Optimizing IO operations to use most efficient available constructs.
D) Exposing an inherently concurrent api to the client that will allow further
   file processing to to occur concurrently.
E) Exposing a non-concurrent api to conservative clients that need minimal changes.
F) Be garbage-collector friendly. Must not be confused with allocating *no* objects,
   but must clients of current API's do repeated allocations of the same objects.
G) Be memory model friendly, use optimal handover algorithms to ensure minimum latency.

Strategies
-------

The project aims to investigate different strategies. The main lines of investigations are as follows:

### Forked single reader thread

![Single Reader Thread Model](images/2Threads.png)


