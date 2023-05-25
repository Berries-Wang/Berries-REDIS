# REDIS SENTINEL
## 为什么需要Redis Sentinel？
&nbsp;&nbsp;了解过 主从复制 可以知道，当Master节点宕机时，Redis就不可用了，因为Master节点无法处理写命令，Replication节点无法更新数据。(Replication节点本身就无法处理写命令)

## 简介
&nbsp;&nbsp;Sentinel 是Redis的高可用性解决方案： 由一个或多个Sentinel实例组成的Sentinel系统可以监视任意多个主服务器，以及这些主服务器属下的所有从服务器，并在被监视的主服务器进入下线状态时，自动将下线的主服务器属下的某个从服务器升级为新的主服务器，然后由新的主服务器代替已下线的主服务器继续处理命令请求。
> 当已下线的主服务器再次上线时，此时会作为新的主服务器的从服务器。

## 启动并初始化Sentinel
### Command
```cmd
    wei@Wang:redis-sentinel /path/to/your/sentinel.conf
     or
    wei@Wang:redis-server  /path/to/your/sentinel.conf --sentinel
```

### 1. 初始化服务器