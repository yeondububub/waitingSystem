package com.example.webflux.service;

import com.example.webflux.exception.QueueErrorCode;
import com.example.webflux.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisRepository redisRepository;

    public Mono<Long> enqueueWaitingQueue(Long userId) {
        long unixTimestamp = Instant.now().getEpochSecond();
        String queue = QueueManager.WAITING_QUEUE.getKey();
        return redisRepository.addZSet(queue, userId, unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(QueueErrorCode.ALREADY_RESISTER_USER.build()))
                .flatMap(i -> redisRepository.zRank(queue, userId))
                .map(i -> i >= 0 ? i + 1 : i);
    }

    public Mono<Long> allow(Long count) {
        return redisRepository.popMin(QueueManager.WAITING_QUEUE.getKey(), count)
                .flatMap(member -> redisRepository.addZSet(
                        QueueManager.PROCEED_QUEUE.getKey(),
                        Long.parseLong(Objects.requireNonNull(member.getValue())),
                        Instant.now().getEpochSecond()
                        )
                )
                .count();
    }

    public Mono<Boolean> isAllowed(Long userId) {
        return redisRepository.zRank(QueueManager.PROCEED_QUEUE.getKey(), userId)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0);
    }
}
