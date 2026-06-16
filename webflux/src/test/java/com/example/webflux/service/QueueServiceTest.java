package com.example.webflux.service;

import com.example.webflux.exception.WaitingQueueException;
import com.example.webflux.config.EmbeddedRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    void setUp() {
        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void enqueueWaitingQueue() {
        StepVerifier.create(queueService.enqueueWaitingQueue(1L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(queueService.enqueueWaitingQueue(2L))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("이미 등록된 유저가 재시도하는 경우 예외를 던진다")
    void alreadyEnqueueWaitingQueue() {
        StepVerifier.create(queueService.enqueueWaitingQueue(1L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(queueService.enqueueWaitingQueue(1L))
                .expectError(WaitingQueueException.class)
                .verify();
    }

    @Test
    @DisplayName("대기열 큐에 유저가 없는 경우 0을 반환한다")
    void emptyAllowUser() {
        StepVerifier.create(queueService.allow(100L))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열 큐에 허용한 유저 수 만큼 카운팅을 반환한다")
    void allowUser() {
        Mono<Long> setup = queueService.enqueueWaitingQueue(1L)
                .then(queueService.enqueueWaitingQueue(2L))
                .then(queueService.enqueueWaitingQueue(3L))
                .then(queueService.enqueueWaitingQueue(4L))
                .then(queueService.enqueueWaitingQueue(5L));

        StepVerifier.create(setup.then(queueService.allow(3L)))
                .expectNext(3L)
                .verifyComplete();
    }

    @DisplayName("허용된 유저가 아니면 false를 반환한다")
    @Test
    void isNotAllowed() {
        StepVerifier.create(queueService.isAllowed(99L))
                .expectNext(false)
                .verifyComplete();
    }

    @DisplayName("대기열 큐에서 허용된 후 다른 아이디로 허용 여부 확인하면 false 반환한다")
    @Test
    void isAllowedOtherUserId() {
        StepVerifier.create(queueService.enqueueWaitingQueue(100L)
                        .then(queueService.allow(3L))
                        .then(queueService.isAllowed(101L)))
                .expectNext(false)
                .verifyComplete();
    }

    @DisplayName("대기열 큐에서 허용된 아이디로 여부 확인하면 true 반환한다")
    @Test
    void isAllowed() {
        Long userId = 100L;

        StepVerifier.create(queueService.enqueueWaitingQueue(userId)
                        .then(queueService.allow(3L))
                        .then(queueService.isAllowed(userId)))
                .expectNext(true)
                .verifyComplete();
    }

}