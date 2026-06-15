package com.example.webflux.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.time.Instant;

@SpringBootTest
@Profile("test")
@Import(EmbeddedRedis.class)
public class RedisRepositoryTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private RedisRepositoryImpl redisRepository;

    @BeforeEach
    void setup() {
        redisRepository = new RedisRepositoryImpl(reactiveRedisTemplate);
        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void addZSet() {
        String queue = "default";
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();

        StepVerifier.create(redisRepository.addZSet(userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void addZSetWhenDuplicate() {
        String queue = "default";
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();

        StepVerifier.create(redisRepository.addZSet(userId, timestamp))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redisRepository.addZSet(userId, timestamp))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void zRank() {
        StepVerifier.create(redisRepository.addZSet(1L, 100L)
                .then(redisRepository.zRank(1L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankWhenMultiUser() {
        StepVerifier.create(redisRepository.addZSet(1L, 100L)
                .then(redisRepository.addZSet(2L, 99L))
                .then(redisRepository.zRank(2L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByNoneUserId() {
        StepVerifier.create(redisRepository.zRank(99L))
                .expectComplete()
                .verify();
    }
}
