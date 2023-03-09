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
#### Redis Command # Set <sup>不可重入锁</sup>
- `127.0.0.1:6379> set key value [EX seconds|PX milliseconds|EXAT timestamp|PXAT milliseconds-timestamp|KEEPTTL] [NX|XX] [GET]` <sup>设置K-V以及过期时间</sup>
    + 参数#Options
      - EX seconds -- Set the specified expire time, in seconds.<sup>设置指定的过期时间，单位:秒</sup>
      - PX milliseconds -- Set the specified expire time, in milliseconds.<sup>设置指定的过期时间，单位:毫秒</sup>
      - EXAT timestamp-seconds -- Set the specified Unix time at which the key will expire, in seconds.<sup>设置Unix系统时间戳作为Key的过期时间，单位:秒</sup>
      - PXAT timestamp-milliseconds -- Set the specified Unix time at which the key will expire, in milliseconds. <sup>设置Unix系统时间戳作为Key的过期时间，单位:毫秒</sup>
      - NX -- Only set the key if it does not already exist.<sup>只有当Key不存在的时候才设置这个Key</sup>
      - XX -- Only set the key if it already exist. <sup>只有当Key存在的时候才设置该Key(当Key不存在时，不会设置值，如 `set 'aa' 123 XX` 返回nil(key aa 不存在),此时 `get 'aa' 返回nil`)</sup>
      - KEEPTTL -- Retain the time to live associated with the key.<sup>保留与Key的存活时间</sup>
      - GET -- Return the old string stored at key, or nil if key did not exist. An error is returned and SET aborted if the value stored at key is not a string.<sup>返回存储的旧值或者在key不存在时返回nil,如果该Key之前不是作为string类型存储的，那么将返回一个error并终止set行为</sup>

### 解锁



---
## 参考资料
1. [Redis Commands#set](https://redis.io/commands/set/)