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
                .expectNext("2ec5dc889cafe9f2af88d4f9ffdc0ea2cf90e5ba2401ceddfac5686a9152ec8d")
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

        // 사용자가 대기열에 등록 및 허용되지 않았고, 토큰도 잘못된 경우
        StepVerifier.create(queueService.isAllowedByToken(userId, token))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("토큰은 올바르나 대기열 진입 허용 상태가 아닌 경우 false를 반환한다")
    void isNotAllowedByTokenWhenNotProceeded() {
        Long userId = 100L;

        Mono<Boolean> test = queueService.generateToken(userId)
                .flatMap(token -> queueService.isAllowedByToken(userId, token));

        StepVerifier.create(test)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("올바른 토큰이고 진입 허용 상태인 경우 허용 여부 확인 시 true를 반환한다")
    void isAllowedByToken() {
        Long userId = 100L;

        Mono<Boolean> test = queueService.enqueueWaitingQueue(userId)
                .then(queueService.allow(1L))
                .then(queueService.generateToken(userId))
                .flatMap(token -> queueService.isAllowedByToken(userId, token));

        StepVerifier.create(test)
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

    @Test
    @DisplayName("만료된 진입 허용 세션을 스케줄러로 정리하면 진입이 불가능해진다")
    void pruneExpiredProceedUsers() {
        Long expiredUserId = 200L;
        Long validUserId = 201L;

        long now = java.time.Instant.now().getEpochSecond();
        long expiredTimestamp = now - 10;
        long validTimestamp = now;

        Mono<Void> setup = reactiveRedisTemplate.opsForZSet().add(QueueManager.PROCEED_QUEUE.getKey(), expiredUserId.toString(), expiredTimestamp)
                .then(reactiveRedisTemplate.opsForZSet().add(QueueManager.PROCEED_QUEUE.getKey(), validUserId.toString(), validTimestamp))
                .then();

        StepVerifier.create(setup)
                .verifyComplete();

        // prune 실행
        queueService.pruneExpiredProceedUsers();

        // expiredUserId는 proceed queue에서 지워졌으므로 isAllowed(userId)가 false여야 함
        StepVerifier.create(queueService.isAllowed(expiredUserId))
                .expectNext(false)
                .verifyComplete();

        // validUserId는 proceed queue에 남아있으므로 isAllowed(userId)가 true여야 함
        StepVerifier.create(queueService.isAllowed(validUserId))
                .expectNext(true)
                .verifyComplete();
    }
}