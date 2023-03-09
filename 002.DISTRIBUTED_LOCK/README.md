# 分布式锁-Redis
## 什么是分布式锁
&nbsp;&nbsp;当多个进程不在同一个系统中，使用分布式锁控制多个进程对资源的访问。

## 分布式锁设计原则
1. 互斥： 在任何指定的时刻，只有一个客户端可以持有锁。
2. 无死锁： 即使锁定资源的客户端崩溃或被分区，也总是可以获得锁。通常超时机制实现。
3. 容错性： 只要大多数Redis节点都启动，客户端就可以获取和释放锁。
4. 同源性:  A加的锁，不能被B解锁
5. 获取锁时非阻塞的: 如果获取不到锁，不能无限期等待。
6. 高性能: 加锁和解锁是高性能的。

## 使用Redis实现分布式锁细节
### 加锁
#### Redis Command
- `set key value [EX seconds|PX milliseconds|EXAT timestamp|PXAT milliseconds-timestamp|KEEPTTL] [NX|XX]`
> 设置K-V以及过期时间
### 解锁