package miltdev.com.rabbitmqdemo.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public boolean acquireLock(String lockKey, String lockValue, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, ttl);

        return Boolean.TRUE.equals(acquired);
    }

    public void releaseLock(String lockKey, String lockValue) {
        String luaScript = """
                  if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                  else
                      return 0
                  end
                  """;

        redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Collections.singletonList(lockKey),
                lockValue
        );
    }
}