# REDIS SENTINEL
## 为什么需要Redis Sentinel？
&nbsp;&nbsp;了解过 主从复制 可以知道，当Master节点宕机时，Redis就不可用了，因为Master节点无法处理写命令，Replication节点无法更新数据。(Replication节点本身就无法处理写命令)