Fast-Dirscan
===========

Based on the apache ant directory scanner as well as the codehaus plexus-utils fork (used
by maven), this code aims to experiment with various concurrent implementations that all intend
to achieve a high degree of mechanical sympathy.

Achieving mechanical sympathy in this area is believed to consist of the following optimizations:

1. Keeping IO subsystem optimally busy; avoiding delays caused by processing
   operations interleaved with IO
2. Avoiding repeated IO at all costs, create an API that will facilitate this in layered architectures.
3. Optimizing IO operations to use most efficient available constructs.
4. Exposing an inherently concurrent api to the client that will allow further
   file processing to to occur concurrently.
5. Exposing a non-concurrent api to conservative clients that need minimal changes (possibly layered on
   top of the concurrent API)
6. Be garbage-collector friendly. Must not be confused with allocating *no* objects,
   but must clients of current API's do repeated allocations of the same objects.
7. Be memory model friendly, use optimal handover algorithms to ensure minimum latency.

Goal
-------

The project aims to create a replacement directory scanner project that can be used by
maven, ant and any other projects using this (or derivate works). The original feature
set is believed to be defined by the ant code, which has been forked a number of times
to handle slight variations.

The project has an apache 2.0 license, and it is my goal to have this project hosted somewhere
at apache eventually if it turns out to be a viable project.

I have no goal of retaining the *full* feature set and extension points of the original ant scanner
or the maven forks, but rather to base the final feature set on what appears to be the common use cases
of this code.


Strategies
-------

The project aims to investigate different strategies, with slightly different implications for client code. The main lines of investigations are as follows:

### Forked single reader thread

![Single Reader Thread Model](images/2Threads.png)

Pros:

1. Can provide client api's that match current api's fairly well.
2. Zero memory model implications for calling application, everything significant happens
   on client thread.

Cons:

1. Overall limits how the client code can be further optimized for threading, we're not just looking
   at a faster way to scan files; but also how to push concurrency into the further processing.
2. Initial latency for creating reader thread.
3. Single reader IO thread may be too limited, does not push IO subsystem to full potential.

Variations:

1. Use multiple reader threads (see below for further discussion).


### SingleReaderMultipleConsumers

![Single Reader Multiple Consumers](images/SingleReaderMultipleConsumers.png)

Pros:

1. Provides an inherently concurrent API to the client, probably facilitating further
   parallel processing in the client.

Cons:

1. Client must be concurrency-aware.


### Multiple reader threads

![Multiple Reader Threads](images/MultipleReaderThreads.png)

Pros:
1. May be able to push IO subsystem further than the single-thread readers.

Cons:
1. May be difficult to determine optimal number of reader threads due to underlying device
  characteristics; creating a general purpose solution may prove hard.
2. It might be necessary to create an alogorithm that needs to be tuned for each individual
   system to achieve optimal performance.

### 2 Thread pools: Multiple Readers, multiple workers.

This last permutation is a combination of the above models; more than on thread reads the disk, and hands
of to a different worker pool. 