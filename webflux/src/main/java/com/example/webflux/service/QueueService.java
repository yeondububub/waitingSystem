package com.example.webflux.service;

import com.example.webflux.exception.QueueErrorCode;
import com.example.webflux.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisRepository redisRepository;

    @Value("${scheduler.enabled}")
    private boolean schedulerEnabled = false;

    @Value("${scheduler.max-allow-user-count}")
    private long maxAllowUserCount = 0;

    @Value("${queue.token.secret-salt}")
    private String tokenSecretSalt;

    public Mono<Long> enqueueWaitingQueue(Long userId) {
        long unixTimestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();
        return redisRepository.addZSetIfAbsent(queue, userId, unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(QueueErrorCode.ALREADY_RESISTER_USER.build()))
                .flatMap(i -> rank(queue, userId));
    }

    public Mono<Long> allow(Long count) {
        return redisRepository.popMin(QueueManager.WAITING_QUEUE.getKey(), count)
                .flatMap(member -> redisRepository.addZSetIfAbsent(
                        QueueManager.PROCEED_QUEUE.getKey(),
                        Long.parseLong(Objects.requireNonNull(member.getValue())),
                        Instant.now().getEpochSecond()
                        )
                )
                .count();
    }

    public Mono<Long> allow(String queue, Long count) {
        return redisRepository.popMin(queue, count)
                .flatMap(
                        member -> redisRepository.addZSetIfAbsent(
                                QueueManager.PROCEED_QUEUE.getKey(),
                                Long.parseLong(Objects.requireNonNull(member.getValue())),
                                Instant.now().getEpochSecond())
                )
                .count();
    }

    public Mono<Boolean> isAllowed(Long userId) {
        return redisRepository.zRank(QueueManager.PROCEED_QUEUE.getKey(), userId)
                .map(rank -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isAllowedByToken(Long userId, String token) {
        return this.generateToken(userId)
                .filter(other -> other.equals(token))
                .flatMap(i -> isAllowed(userId))
                .defaultIfEmpty(false);
    }

    public Mono<Long> checked(Long userId) {
        return enqueueWaitingQueue(userId)
                .onErrorResume(e -> rank(QueueManager.WAITING_QUEUE.getKey(), userId));
    }

    public Mono<Long> rank(String queue, Long userId) {
        return redisRepository.zRank(queue, userId)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    private void allowWaitQueueUser() {
        if (!schedulerEnabled) {
            log.info("passed scheduling");
            return;
        }

        log.info("process scheduling");
        redisRepository.scan("wait:*", 100L)
                .flatMap(queue -> allow(queue, maxAllowUserCount)
                        .map(allowedCount -> Tuple.of(queue.getBytes(), allowedCount.doubleValue())))
                .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue"
                        .formatted(maxAllowUserCount, tuple.getScore().longValue(), new String(tuple.getValue()))))
                .subscribe();
    }

    public Mono<String> generateToken(Long userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = "user-queue-%d-%s".formatted(userId, tokenSecretSalt);
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                hexString.append(String.format("%02x", b));
            }
            return Mono.just(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
