package com.example.webflux.repository;

import com.example.webflux.config.EmbeddedRedis;
import com.example.webflux.service.QueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
    void addZSetIfAbsent() {
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void addZSetIfAbsentWhenDuplicate() {
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, timestamp))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void zRank() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, 1L, 100L)
                .then(redisRepository.zRank(queue, 1L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankWhenMultiUser() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, 1L, 100L)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, 99L))
                .then(redisRepository.zRank(queue, 2L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void zRankByNoneUserId() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.zRank(queue, 99L))
                .expectComplete()
                .verify();
    }

    @Test
    void popMin() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        long timestamp = Instant.now().getEpochSecond();

        Mono<Boolean> setup = redisRepository.addZSetIfAbsent(queue, 1L, timestamp)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, timestamp + 1L))
                .then(redisRepository.addZSetIfAbsent(queue, 3L, timestamp + 2L));

        Flux<ZSetOperations.TypedTuple<String>> result = setup.thenMany(redisRepository.popMin(queue, 3L));

        StepVerifier.create(result)
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("1"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("2"))
                .expectNextMatches(tuple -> tuple.getValue().equalsIgnoreCase("3"))
                .verifyComplete();
    }
}
