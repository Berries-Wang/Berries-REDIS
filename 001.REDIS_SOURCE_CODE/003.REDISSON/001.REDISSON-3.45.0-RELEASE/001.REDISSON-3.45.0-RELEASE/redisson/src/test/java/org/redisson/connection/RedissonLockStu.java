package org.redisson.connection;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;

public class RedissonLockStu extends AbstractRedissonSingleton {
    /**
     * 测试 使用Redisson实现分布式锁
     */
    @Test
    public void testRedissonLock() throws InterruptedException {
        // 创建一把锁
        RLock test_lock = redisson.getLock("Wei.Wang-Test001");
        // 尝试加锁: 可重入(使用的应用类型: hash)
        // if (test_lock.tryLock(1000, TimeUnit.MILLISECONDS)) { // 尝试加锁，加锁失败会订阅,锁释放是会通知
        if (test_lock.tryLock()) { // 直接尝试加锁
            System.out.println("加锁成功");
            int i = 0;
            while ((i++) < 10000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            // 释放锁
            test_lock.unlock();
            return;
        }
        System.out.println("加锁失败");
    }
}
