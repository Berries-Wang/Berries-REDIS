# Redis 主从复制
&nbsp;&nbsp;Redis 如何使用备份来支持高可用和故障转移

## 摘要
```txt
  1. 在主从模式下(无集群，无Sentinel)，主服务器下线，不会有故障转移操作，即不会有新的master产生.
```

&nbsp;&nbsp;在Redis复制的基础上(不包括Redis Cluster或Redis Sentinel提供的高可用性特性)，有一个leader - follower(主-副本)复制，它的使用和配置都很简单。它允许复制Redis实例是主实例的精确副本。每次链接断开时，副本都会自动重新连接到主服务器，并且无论主服务器发生了什么，都会尝试成为它的精确副本。

&nbsp;&nbsp;该系统主要通过三个机制工作:
1. When a master and a replica instances are well-connected, the master keeps the replica updated by sending a stream of commands to the replica to replicate the effects on the dataset happening in the master side due to: client writes, keys expired or evicted, any other action changing the master dataset.
   > 当主从连接良好的时候，主机通过发送命令流来复制由于以下原因在主机中发生的对数据集合的影响: 客户端写入、Key过期或被驱逐，以及任何修改主机数据集合的操作。
2. When the link between the master and the replica breaks, for network issues or because a timeout is sensed in the master or the replica, the replica reconnects and attempts to proceed with a partial resynchronization: it means that it will try to just obtain the part of the stream of commands it missed during the disconnection.
   > 当主从之间的连接因为网络原因或发送数据超时而断开了，副本会重新连接并且尝试部分再同步处理: 这意味着他将尝试获取的仅仅是断开连接之间的部分命令流。
3. When a partial resynchronization is not possible, the replica will ask for a full resynchronization. This will involve a more complex process in which the master needs to create a snapshot of all its data, send it to the replica, and then continue sending the stream of commands as the dataset changes.
   > 当部分再同步不可用的时候，副本会请求全量再同步。这将涉及一个更复杂的过程，其中主服务器需要创建其所有数据的快照，将其发送到副本，然后在数据集更改时继续发送命令流。

&nbsp;&nbsp;Redis默认使用异步复制，他具有低延迟高性能的特点，是绝大多数Redis用例的默认复制模式。然而，Redis副本会周期性的向主服务器异步确认收到的数据量，因此，主服务器不需要每次等待处理副本的命令，但是如果需要，他知道哪个副本已经处理了哪个命令。这允许有可选的同步复制

&nbsp;&nbsp;Synchronous replication of certain data can be requested by the clients using the WAIT command. However WAIT is only able to ensure there are the specified number of acknowledged copies in the other Redis instances, it does not turn a set of Redis instances into a CP system with strong consistency: acknowledged writes can still be lost during a failover, depending on the exact configuration of the Redis persistence. However with WAIT the probability of losing a write after a failure event is greatly reduced to certain hard to trigger failure modes.
  > 客户端可以使用WAIT命令来请求某些数据的同步复制，然而，WAIT只能够确保其他Redis实例中有指定数量的已确认副本，他不会将一组Redis实例变成具有强一致性的CP系统： 已确认的写入在故障转移期间仍然可能丢失，具体取决于持久化的确切配置，然而，使用WAIT后，在发生故障事件后丢失写入的可能性大大降低到某些难以触发的故障模式。

## Important facts<sup>设备管理控制与时间调度程序；</sup> about Redis replication
- Redis uses asynchronous replication, with asynchronous replica-to-master acknowledges of the amount of data processed.
  > Redis 使用异步复制，副本异步向主节点确认处理的数据量。
- A master can have multiple replicas.
  > 一个主节点可以有多个副本
- Replicas are able to accept connections from other replicas. Aside from connecting a number of replicas to the same master, replicas can also be connected to other replicas in a cascading-like structure. Since Redis 4.0, all the sub-replicas will receive exactly the same replication stream from the master.
  > 副本可以接受来自其他副本的连接。除了将多个副本连接到同一个主节点外，副本还能够以类似于级联的结构连接到其他副本。
- Redis replication is non-blocking on the master side. This means that the master will continue to handle queries when one or more replicas perform the initial synchronization or a partial resynchronization.
  > Redis 复制在master侧是异步的。这意味着master在一个或多个副本执行初始备份和持续备份期间将继续处理查询。
- Replication is also largely(很大程度上) non-blocking on the replica side. While the replica is performing the initial synchronization, it can handle queries using the old version of the dataset, assuming you configured Redis to do so in redis.conf. Otherwise, you can configure Redis replicas to return an error to clients if the replication stream is down. However, after the initial sync, the old dataset must be deleted and the new one must be loaded. The replica will block incoming connections during this brief(短暂的;简短的;) window (that can be as long as many seconds for very large datasets). Since Redis 4.0 you can configure Redis so that the deletion of the old data set happens in a different thread, however loading the new initial dataset will still happen in the main thread and block the replica.
  > 即 副本在很大程度上也是异步的，但是在短暂的时间窗口中会阻塞查询。

- Replication can be used both for scalability(可扩展性；可伸缩性；可量测性), to have multiple replicas for read-only queries (for example, slow O(N) operations can be offloaded(v.卸货;即将只读操作交由副本执行) to replicas), or simply for improving data safety and high availability.
  > 即 副本也可以提供查询服务

- You can use replication to avoid the cost of having the master writing the full dataset to disk: a typical technique involves(包含；牵涉（involve 的第三人称单数）) configuring your master redis.conf to avoid persisting(v.坚持，persist的ing形式) to disk at all, then connect a replica configured to save from time to time(time to time: 不时地；时常), or with AOF enabled. However, this setup must be handled with care, since a restarting master will start with an empty dataset: if the replica tries to sync with it, the replica will be emptied as well.
  > 可以使用副本机制来避免将数据持久化磁盘，但是要慎用

## Safety of replication when master has persistence turned off <sup>当master持久化关闭时复制的安全性</sup>
&nbsp;&nbsp;In setups where Redis replication is used, it is strongly advised to have persistence turned on in the master and in the replicas. When this is not possible, for example because of latency(潜伏；潜在因素) concerns(涉及;与..相关) due to very slow disks, instances should be configured to avoid restarting automatically after a reboot.

&nbsp;&nbsp;To better understand why masters with persistence turned off configured to auto restart are dangerous, check the following failure mode where data is wiped from the master and all its replicas:
> 更好地理解为什么master持久化关闭时自动重启是个危险的操作
> > 即 若主节点自动重启了，那么副本会再次从主节点同步，那么副本的所有数据都会被清理掉.

1. We have a setup with node A acting as master, with persistence turned down, and nodes B and C replicating from node A.
   > 环境搭建
2. Node A crashes(崩溃), however it has some auto-restart system, that restarts the process. However since persistence is turned off, the node restarts with an empty data set.
   > 
3. Nodes B and C will replicate from node A, which is empty, so they'll effectively destroy their copy of the data.

## How Redis replication works
&nbsp;&nbsp;Every Redis master has a replication ID: it is a large pseudo(假的，伪装的) random string that marks a given story of the dataset. Each master also takes an offset that increments for every byte of replication stream that it is produced to be sent to replicas, to update the state of the replicas with the new changes modifying the dataset. The replication offset is incremented even if no replica is actually connected, so basically every given pair of:
> 每个master都有一个'replication ID'： 他是一个大的伪随机字符串，用来标记数据集的变化。每个master还有一个偏移量，该偏移量将根据发送给副本的复制流增加，以便将数据集最新的状态更新到副本。即使没有副本连接，复制偏移量也会增加。<sup>重点，有没有副本，偏移量还是会增加的</sup>
```txt
   Replication ID, offset
```

&nbsp;&nbsp;When replicas connect to masters, they use the PSYNC command to send their old master replication ID and the offsets they processed so far. This way the master can send just the incremental part needed. However if there is not enough backlog in the master buffers, or if the replica is referring to an history (replication ID) which is no longer known, then a full resynchronization happens: in this case the replica will get a full copy of the dataset, from scratch.(from scratch: 白手起家；从头做起)
> 全量复制触发: 当副本连接到master,他们会使用PSYNC命令发送他们目前为止处理过的replication ID 和偏移量，按照这种方式，master可以仅发送增量的部分。然而，如果在master的缓冲区没有足够的备份日志 或者 副本使用了不再使用的replication ID和偏移量，然后就会执行全量再同步： 在这种情况下，副本将会得到全量的数据副本，

&nbsp;&nbsp;This is how a full synchronization works in more details:

&nbsp;&nbsp;The master starts a background saving process to produce an RDB file. At the same time it starts to buffer all new write commands received from the clients. When the background saving is complete, the master transfers the database file to the replica, which saves it on disk, and then loads it into memory. The master will then send all buffered commands to the replica. This is done as a stream of commands and is in the same format of the Redis protocol itself.
> 即 主节点发送RDB file 和 命令缓存给副本

&nbsp;&nbsp;You can try it yourself via telnet. Connect to the Redis port while the server is doing some work and issue the SYNC command. You'll see a bulk transfer and then every command received by the master will be re-issued in the telnet session. Actually SYNC is an old protocol no longer used by newer Redis instances, but is still there for backward(向后) compatibility(共存): it does not allow partial resynchronizations, so now PSYNC is used instead.
> 你可以亲自通过telnet命令尝试。在Redis Server做一些事并发出SYNC命令的时候连接到Redis,您将看到一个批量传输，然后主机接收到的每个命令都将在telnet会话中重新发出. SYNC 是旧的协议，被PSYNC替代，但是向后兼容，SYNC依旧可以使用。

&nbsp;&nbsp;As already said, replicas are able to automatically reconnect when the master-replica link goes down for some reason. If the master receives multiple concurrent replica synchronization requests, it performs a single background save in to serve all of them.
> 之前说过，副本可以在master-replica链接断开时自动重连。如果master收到了多个副本并发同步请求，它执行一个后台保存来为所有这些服务。

## Replication ID explained
&nbsp;&nbsp;In the previous section we said that if two instances have the same replication ID and replication offset, they have exactly(精确的;恰好;完全) the same data. However it is useful to understand what exactly is the replication ID, and why instances have actually two replication IDs: the main ID and the secondary ID.

&nbsp;&nbsp;A replication ID basically(基本上) marks a given history of the data set. Every time an instance restarts from scratch(from scratch:白手起家) as a master, or a replica is promoted(促进；宣传；促销；提升；将（体育运动队）晋级（promote 的过去式和过去分词）) to master, a new replication ID is generated for this instance. The replicas connected to a master will inherit(继承) its replication ID after the handshake. So two instances with the same ID are related by the fact that they hold the same data, but potentially(可能地，潜在地) at a different time. It is the offset that works as a logical(合情合理的；合乎逻辑的；) time to understand, for a given history (replication ID), who holds the most updated data set.

&nbsp;&nbsp;For instance, if two instances A and B have the same replication ID, but one with offset 1000 and one with offset 1023, it means that the first lacks(缺乏;不足;) certain commands applied to the data set. It also means that A, by applying just a few commands, may reach exactly the same state of B.
> 例如，如果两个实例A和B具有相同的复制ID，但是一个实例的偏移量为1000，另一个实例的偏移量为1023，则意味着第一个实例缺少应用于数据集的某些命令。这也意味着，只需应用几个命令，A就可以达到与B完全相同的状态。

&nbsp;&nbsp;The reason why Redis instances have two replication IDs is because of replicas that are promoted to masters. After a failover(故障切换；), the promoted replica requires to still remember what was its past replication ID, because such replication ID was the one of the former(旧时的；以前的) master. In this way, when other replicas will sync with the new master, they will try to perform a partial resynchronization using the old master replication ID. This will work as expected, because when the replica is promoted to master it sets its secondary ID to its main ID, remembering what was the offset when this ID switch happened. Later it will select a new random replication ID, because a new history begins. When handling the new replicas connecting, the master will match their IDs and offsets both with the current ID and the secondary ID (up to a given offset, for safety). In short this means that after a failover, replicas connecting to the newly promoted master don't have to perform a full sync.
> 为什么Redis实例有两个replication IDs是因为副本晋升为master.在故障转移之后，提升的副本仍然需要记住它过去的复制ID，因为这个复制ID是以前的主副本的复制ID。这样，当其他副本与新的主复制同步时，它们将尝试使用旧的主复制ID执行部分重同步。这将如预期的那样工作，因为当副本提升为主副本时，它将其辅助ID设置为其主ID，并记住发生此ID切换时的偏移量。稍后，它将选择一个新的随机复制ID，因为新的历史记录开始了。当处理新的副本连接时，主服务器将用当前ID和辅助ID匹配它们的ID和偏移量(为了安全起见，最多匹配给定的偏移量)。简而言之，这意味着在故障转移之后，连接到新提升的主服务器的副本不必执行完全同步。

&nbsp;&nbsp;In case you wonder(好奇;) why a replica promoted to master needs to change its replication ID after a failover: it is possible that the old master is still working as a master because of some network partition: retaining(保留;) the same replication ID would violate(违反，违背；侵犯，打扰；) the fact that the same ID and same offset of any two random instances mean they have the same data set.
> 为什么副本晋升为master需要改变replication ID,因为原先的master可能还在线，这样会违背数据的一致性.

## Diskless replication
&nbsp;&nbsp;Normally a full resynchronization requires creating an RDB file on disk, then reloading the same RDB from disk to feed the replicas with the data.
> 一般情况

&nbsp;&nbsp;With slow disks this can be a very stressing operation for the master. Redis version 2.8.18 is the first version to have support for diskless replication. In this setup the child process directly sends the RDB over the wire to replicas, without using the disk as intermediate storage.
> 无磁盘同步


## Configuration
&nbsp;&nbsp;To configure basic Redis replication is trivial(琐碎的，不重要的；容易解决的，不费吹灰之力的；): just add the following line to the replica configuration file:
```txt
   replicaof 192.168.1.1 6379
```
- Of course you need to replace 192.168.1.1 6379 with your master IP address (or hostname) and port. Alternatively(要不，或者), you can call the REPLICAOF command and the master host will start a sync with the replica.
- There are also a few parameters for tuning the replication backlog taken in memory by the master to perform the partial resynchronization. See the example redis.conf shipped with the Redis distribution for more information.
- Diskless replication can be enabled using the repl-diskless-sync configuration parameter. The delay to start the transfer to wait for more replicas to arrive after the first one is controlled by the repl-diskless-sync-delay parameter. Please refer to the example redis.conf file in the Redis distribution for more details.

## Read-only replica
&nbsp;&nbsp;Since Redis 2.6, replicas support a read-only mode that is enabled by default. This behavior is controlled by the replica-read-only option in the redis.conf file, and can be enabled and disabled at runtime using CONFIG SET.

&nbsp;&nbsp;Read-only replicas will reject all write commands, so that it is not possible to write to a replica because of a mistake. This does not mean that the feature(特点;特征;) is intended(预期;打算;) to expose a replica instance to the internet or more generally(笼统地，大概) to a network where untrusted(不信任的) clients exist, because administrative(管理的，行政的) commands like DEBUG or CONFIG are still enabled. The Security page describes how to secure a Redis instance.
> Read-only副本拒绝所有的写命令，但是调试命令还是可以执行的

&nbsp;&nbsp;You may wonder why it is possible to revert(恢复;回复;) the read-only setting and have replica instances that can be targeted by write operations. The answer is that writable replicas exist only for historical(历史的;) reasons. Using writable replicas can result in inconsistency(不一致;) between the master and the replica, so it is not recommended(被推荐的;) to use writable replicas. To understand in which situations(状况；情境；局面（situation 的复数形式）) this can be a problem, we need to understand how replication works. Changes on the master is replicated by propagating(传播的；繁殖的) regular(通常的;) Redis commands to the replica. When a key expires on the master, this is propagated as a DEL command. If a key which exists on the master but is deleted, expired or has a different type on the replica compared to the master will react differently to commands like DEL, INCR or RPOP propagated from the master than intended. The propagated command may fail on the replica or result in a different outcome. To minimize the risks(风险，危险（risk 复数）) (if you insist on using writable replicas) we suggest you follow these recommendations:
> 讲述了 副本是如何工作的——master讲redis 命令同步到副本.
> > 为什么不能使用可写的副本?因为会造成数据的不一致性.
- Don't write to keys in a writable replica that are also used on the master. (This can be hard to guarantee(确保;保证;) if you don't have control over all the clients that write to the master.)
- Don't configure an instance as a writable replica as an intermediary step when upgrading a set of instances in a running system. In general, don't configure an instance as a writable replica if it can ever be promoted to a master if you want to guarantee data consistency.

&nbsp;&nbsp;Historically, there were some use cases that were considered legitimate(正当的，合理的；) for writable replicas. As of version 7.0, these use cases are now all obsolete(淘汰的，废弃的；) and the same can be achieved(达到，取得；完成；) by other means. For example:
> 历史上，有一些用例被认为是合法的可写副本。从7.0版本开始，这些用例现在都过时了，可以通过其他方式实现。例如:
- Computing slow Set or Sorted set operations and storing the result in temporary local keys using commands like SUNIONSTORE and ZINTERSTORE. Instead, use commands that return the result without storing it, such as SUNION and ZINTER.
- Using the SORT command (which is not considered a read-only command because of the optional STORE option and therefore cannot be used on a read-only replica). Instead, use SORT_RO, which is a read-only command.
- Using EVAL and EVALSHA are also not considered read-only commands, because the Lua script may call write commands. Instead, use EVAL_RO and EVALSHA_RO where the Lua script can only call read-only commands.

&nbsp;&nbsp;While writes to a replica will be discarded(丢弃的；废弃的) if the replica and the master resync or if the replica is restarted, there is no guarantee(确保) that they will sync automatically.

&nbsp;&nbsp;Before version 4.0, writable replicas were incapable(不能的；无能力的；不能胜任的) of expiring keys with a time to live set. This means that if you use EXPIRE or other commands that set a maximum TTL for a key, the key will leak, and while you may no longer see it while accessing it with read commands, you will see it in the count of keys and it will still use memory. Redis 4.0 RC3 and greater versions are able to evict(驱逐，逐出) keys with TTL as masters do, with the exceptions of keys written in DB numbers greater than 63 (but by default Redis instances only have 16 databases). Note(注意) though(虽然) that even in versions greater than 4.0, using EXPIRE on a key that could ever exists on the master can cause inconsistency(不一致；易变) between the replica and the master.
> 在4.0前，可写的副本无法处理过期的Key，会造成内存泄露。在4.0 RC3后，可写副本可以像master一样驱逐Key，除了DB号大于63的键。但是需要注意，在master上对Key上使用Expire依旧会造成数据不一致的现象。

&nbsp;&nbsp;Also note that since Redis 4.0 replica writes are only local, and are not propagated to sub-replicas attached to the instance. Sub-replicas instead will always receive the replication stream identical to the one sent by the top-level master to the intermediate replicas. So for example in the following setup:
> 即 副本的写入是在本地，不会传播到下一级。
```txt
    A ---> B ---> C
```
- Even if B is writable, C will not see B writes and will instead have identical dataset as the master instance A.

## How Redis replication deals with expires on keys
&nbsp;&nbsp;Redis expires allow keys to have a limited time to live (TTL). Such a feature depends on the ability of an instance to count the time, however Redis replicas correctly replicate keys with expires, even when such keys are altered(改变，改动，修改；阉割，切除（alter 的过去式过去分词）) using Lua scripts.
> 副本完全复制

&nbsp;&nbsp;To implement such a feature Redis cannot rely on the ability of the master and replica to have synced clocks, since this is a problem that cannot be solved and would result in race conditions and diverging data sets, so Redis uses three main techniques to make the replication of expired keys able to work:
> 为了实现这样的功能，Redis不能依赖于主和副本同步时钟的能力，因为这是一个无法解决的问题，会导致竞争条件和分散的数据集，所以Redis使用三种主要技术来使过期密钥的复制能够工作:
1. Replicas don't expire keys, instead they wait for masters to expire the keys. When a master expires a key (or evict it because of LRU), it synthesizes a DEL command which is transmitted to all the replicas.
   > 副本同步master的删除命令
2. However because of master-driven expire, sometimes replicas may still have in memory keys that are already logically expired, since the master was not able to provide the DEL command in time. To deal with that the replica uses its logical clock to report that a key does not exist only for read operations that don't violate the consistency of the data set (as new commands from the master will arrive). In this way replicas avoid reporting logically expired keys that are still existing. In practical terms(in practical terms:实际上；在实践中), an HTML fragments cache that uses replicas(复制品) to scale will avoid returning items that are already older than the desired(期望得到;) time to live.
   > 逻辑上过期了，master不能够及时提供DEL命令。为了解决这个问题，副本在不违背一致性的前提下使用自身的逻辑时钟来报告Key不存在。
3. During Lua scripts executions no key expiries are performed. As a Lua script runs, conceptually(概念上) the time in the master is frozen(结冰；凝固), so that a given key will either exist or not for all the time the script runs. This prevents keys expiring in the middle of a script, and is needed to send the same script to the replica in a way that is guaranteed to have the same effects in the data set.
   > 在lua脚本执行期间，不执行任何键过期。发送相同的lua脚本到副本


&nbsp;&nbsp;Once a replica is promoted to a master it will start to expire keys independently(独立地；自立地), and will not require any help from its old master.

## Partial sync after restarts and failovers
&nbsp;&nbsp;Since Redis 4.0, when an instance is promoted to master after a failover, it will still be able to perform a partial resynchronization with the replicas of the old master. To do so, the replica remembers the old replication ID and offset of its former(旧时的；以前的) master, so can provide part of the backlog to the connecting replicas even if they ask for the old replication ID.
> 从Redis 4.0开始，当一个实例在故障转移后被提升为master时，它仍然能够与旧master的副本执行部分重同步。为此，副本会记住其前主副本的旧复制ID和偏移量，因此即使连接的副本请求旧复制ID，也可以向它们提供部分积压任务。

&nbsp;&nbsp;However the new replication ID of the promoted replica will be different, since it constitutes(构成;组成;包含;) a different history of the data set. For example, the master can return available and can continue accepting writes for some time, so using the same replication ID in the promoted replica would violate the rule that a replication ID and offset pair identifies only a single data set.
> 但是，提升副本的新复制ID将是不同的，因为它构成了数据集的不同历史。例如，主服务器可以返回可用，并且可以在一段时间内继续接受写操作，因此在提升副本中使用相同的复制ID将违反复制ID和偏移量对仅标识单个数据集的规则。

&nbsp;&nbsp;Moreover(此外), replicas - when powered off gently(文静地，温柔地；轻柔地；) and restarted - are able to store in the RDB file the information needed to resync with their master. This is useful in case of upgrades. When this is needed, it is better to use the SHUTDOWN command in order to perform a save & quit operation on the replica.
> 此外，副本(当轻轻地关闭电源并重新启动时)能够在RDB文件中存储与它们的主服务器重新同步所需的信息。这在升级的情况下很有用。当需要这样做时，最好使用SHUTDOWN命令，以便在副本上执行保存和退出操作。

&nbsp;&nbsp;It is not possible to partially(不完全地，部分地；) sync a replica that restarted via the AOF file. However the instance may be turned to RDB persistence before shutting down it, than can be restarted, and finally AOF can be enabled again.
> 不可能对通过AOF文件重新启动的副本进行部分同步。但是，在关闭实例之前，实例可以转为RDB持久性，然后可以重新启动实例，最后可以再次启用AOF。



---
## 参考资料
1. [https://redis.io/docs/management/replication/](https://redis.io/docs/management/replication/)