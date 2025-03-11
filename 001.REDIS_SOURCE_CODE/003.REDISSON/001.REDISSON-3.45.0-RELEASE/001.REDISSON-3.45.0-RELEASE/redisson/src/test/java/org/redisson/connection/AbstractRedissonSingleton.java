package org.redisson.connection;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Protocol;

public class AbstractRedissonSingleton {
    public static final RedissonClient redisson;

    static {
        Config config = createConfig();
        redisson = Redisson.create(config);
    }

    private static Config createConfig() {
        Config config = new Config();
        config.setProtocol(Protocol.RESP2);
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        return config;
    }
}
