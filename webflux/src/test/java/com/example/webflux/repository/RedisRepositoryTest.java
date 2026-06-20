package com.example.webflux.repository;

import com.example.webflux.config.EmbeddedRedis;
import com.example.webflux.service.QueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;


@SpringBootTest
@ActiveProfiles("test")
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
    @DisplayName("대기열 큐에 사용자가 없으면 성공적으로 등록한다.")
    void addZSetIfAbsent() {
        Long userId = 1L;
        long timestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, userId, timestamp))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열 큐에 등록된 사용자의 경우 false를 반환한다.")
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
    @DisplayName("대기열 큐에서 사용자의 순위(0-based)를 조회한다.")
    void zRank() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, 1L, 100L)
                .then(redisRepository.zRank(queue, 1L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("여러 사용자가 대기열에 등록되었을 때 타임스탬프 순서대로 순위를 조회한다.")
    void zRankWhenMultiUser() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsent(queue, 1L, 100L)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, 99L))
                .then(redisRepository.zRank(queue, 2L)))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열 큐에 등록되지 않은 사용자의 순위를 조회하면 빈 값을 반환한다.")
    void zRankByNoneUserId() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.zRank(queue, 99L))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("대기열 큐에서 가장 오래 대기한 지정된 수만큼의 사용자들을 제거하며 가져온다.")
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

    @Test
    @DisplayName("패턴에 매칭되는 큐 키를 조회할 때 데이터가 없으면 빈 리스트를 반환한다.")
    void scanWithEmpty() {
        String pattern = "wait:*";
        Long count = 100L;

        StepVerifier.create(redisRepository.scan(pattern, count).collectList())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    @DisplayName("패턴에 매칭되는 큐 키를 조회할 때 매칭되는 큐 키들을 반환한다.")
    void scanWhenExistQueueData() {
        String queue = QueueManager.WAITING_QUEUE.getKey();
        String pattern = "wait:*";
        Long count = 100L;

        long timestamp = Instant.now().getEpochSecond();

        Mono<Boolean> setup = redisRepository.addZSetIfAbsent(queue, 1L, timestamp)
                .then(redisRepository.addZSetIfAbsent(queue, 2L, timestamp + 1L))
                .then(redisRepository.addZSetIfAbsent(queue, 3L, timestamp + 2L));

        StepVerifier.create(setup.thenMany(redisRepository.scan(pattern, count).collectList()))
                .expectNextMatches(list -> list.contains(queue))
                .verifyComplete();
    }

    @Test
    @DisplayName("Lua 스크립트를 사용하여 대기열 등록과 대기 순위 조회를 원자적으로 수행한다.")
    void luaScript() {
        String queue = QueueManager.WAITING_QUEUE.getKey();

        StepVerifier.create(redisRepository.addZSetIfAbsentAndRank(queue, 1L, 100L)
                        .then(redisRepository.addZSetIfAbsentAndRank(queue, 2L, 101L))
                        .then(redisRepository.addZSetIfAbsentAndRank(queue, 3L, 102L))
                        .then(redisRepository.addZSetIfAbsentAndRank(queue, 4L, 103L))
                )
                .expectNext(3L)
                .verifyComplete();

    }
}
