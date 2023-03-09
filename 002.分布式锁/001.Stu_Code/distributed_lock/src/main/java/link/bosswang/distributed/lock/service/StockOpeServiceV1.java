package link.bosswang.distributed.lock.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 库存扣减： 1.0版本
 */
@Service
public class StockOpeServiceV1 {

    @Autowired
    private RedisTemplate redisTemplate;

    public void stockReduce() {
        redisTemplate.opsForValue().setIfAbsent()
    }
}
