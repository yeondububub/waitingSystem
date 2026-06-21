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
    @DisplayName("대기열 큐에 유저를 추가하면 대기 순번을 응답한다")
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

    @Test
    @DisplayName("토큰 생성 검증")
    void generateToken() {
        Long userId = 100L;

        StepVerifier.create(queueService.generateToken(userId))
                .expectNext("2d5d9b49e5991835ad5080d8d68ad34f43edd862df267bc2fa82bba5eb31135f")
                .verifyComplete();
    }

    @Test
    @DisplayName("처리한 대기열 유저의 개수를 반환한다.")
    void allowUserByCount() {
        String queue = QueueManager.WAITING_QUEUE.getKey();
        Long count = 100L;

        Mono<Long> setup = queueService.enqueueWaitingQueue(1L)
                .then(queueService.enqueueWaitingQueue(2L))
                .then(queueService.enqueueWaitingQueue(3L))
                .then(queueService.enqueueWaitingQueue(4L))
                .then(queueService.enqueueWaitingQueue(5L));

        StepVerifier.create(setup.then(queueService.allow(count)))
                .expectNext(5L)
                .verifyComplete();
    }

    @Test
    @DisplayName("올바르지 않은 토큰인 경우 허용 여부 확인 시 false를 반환한다")
    void isNotAllowedByToken() {
        Long userId = 100L;
        String token = "wrong-token";

        StepVerifier.create(queueService.isAllowedByToken(userId, token))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("올바른 토큰인 경우 허용 여부 확인 시 true를 반환한다")
    void isAllowedByToken() {
        Long userId = 100L;
        String token = "2d5d9b49e5991835ad5080d8d68ad34f43edd862df267bc2fa82bba5eb31135f";

        StepVerifier.create(queueService.isAllowedByToken(userId, token))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열에 없는 유저인 경우 -1를 반환한다")
    void checkedWhenNoneRegisterUserId() {
        String queue = QueueManager.WAITING_QUEUE.getKey();
        Long userId = 99L;

        StepVerifier.create(queueService.rank(queue, userId))
                .expectNext(-1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열에 있는 유저의 경우 대기열 순위를 반환한다")
    void checkedWhenExistWaitingQueue() {
        String queue = QueueManager.WAITING_QUEUE.getKey();
        Long userId = 101L;

        StepVerifier.create(queueService.enqueueWaitingQueue(100L)
                        .then(queueService.enqueueWaitingQueue(userId))
                        .then(queueService.enqueueWaitingQueue(102L))
                        .then(queueService.rank(queue, userId)))
                .expectNext(2L)
                .verifyComplete();
    }
}