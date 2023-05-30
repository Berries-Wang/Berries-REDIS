# Redis 集群

## 开启集群模式
&nbsp;&nbsp;一个Redis集群节点就是一个运行在集群模式下的Redis服务器，Redis服务器在启动时会根据cluster-ebabled配置选项是否为yes来决定是否开启服务器的集群模式.
```txt
    cluster-enabled yes
```