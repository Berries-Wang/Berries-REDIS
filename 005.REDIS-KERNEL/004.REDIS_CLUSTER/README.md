# Redis 集群

## 开启集群模式
&nbsp;&nbsp;一个Redis集群节点就是一个运行在集群模式下的Redis服务器，Redis服务器在启动时会根据cluster-ebabled配置选项是否为yes来决定是否开启服务器的集群模式.
```txt
    cluster-enabled yes
```

## CLUSTER MEET 命令
&nbsp;&nbsp;通过向节点A发送CLUSTER MEET命令，客户端可以让接收命令的节点A将另一个节点B添加到节点A当前所在的集群中。
```TXT
    CLUSTER MEET <IP> <PORT>
```

## 槽指派
&nbsp;&nbsp;Redis集群通过分片的方式保存数据库中的键值对：集群中的整个数据库被分为16384个槽(slot),数据库中的每个键都属于这16384个槽中的一个，集群中的每个节点可以处理0或最多16384个槽。
- 当数据库中的16384个槽都有节点在处理时，集群处于上线状态；相反，若存在任意一个槽没有被处理，那么集群处于下线状态。
```txt
    wei@Wang:~/WorkSpace/open_source/Redis/001.REDIS_SOURCE_CODE/redis-6.2.5$ ./src/redis-cli -p 6385
       127.0.0.1:6385> cluster info
       cluster_state:fail
       cluster_slots_assigned:0
       cluster_slots_ok:0
       cluster_slots_pfail:0
       cluster_slots_fail:0
       cluster_known_nodes:1
       cluster_size:0
       cluster_current_epoch:0
       cluster_my_epoch:0
       cluster_stats_messages_sent:0
       cluster_stats_messages_received:0
    127.0.0.1:6385> 
```

&nbsp;&nbsp;通过命令CLUSTER ADDSLOTS命令(在指定的节点上执行)，可以将一个或多个槽指派给节点负责。
```txt
    CLUSTER ADDSLOTS 0 1 2 3 4 5 ... 5000
    #001.REDIS_SOURCE_CODE/redis-6.2.5/src/cluster.h#'struct clusterNode'
```

## 传播节点的槽指派信息 && 记录集群所有槽的指派信息
&nbsp;&nbsp;一个节点除了会将自己负责的槽记录再clusterNode结构的slots属性和numslots属性之外，还会将自己的slots数组通过消息发送给集群中的其他节点。

&nbsp;&nbsp;clusterState结构中的slots数组记录了集群中所有16384个槽的指派信息：
```c
   // 001.REDIS_SOURCE_CODE/redis-6.2.5/src/cluster.h
   typedef struct clusterState {
      // ...
      clusterNode *slots[CLUSTER_SLOTS];
      // ...
    } clusterState;
    // slots[i] == NULL?‘该槽还未被指派’：槽已经被指派给了clusterNode所代表的节点。
```