package com.example.webflux.service;

import com.example.webflux.exception.QueueErrorCode;
import com.example.webflux.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final RedisRepository redisRepository;

    public Mono<Long> enqueueWaitingQueue(Long userId) {
        long unixTimestamp = Instant.now().getEpochSecond();
        return redisRepository.addZSet(userId, unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(QueueErrorCode.ALREADY_RESISTER_USER.build()))
                .flatMap(i -> redisRepository.zRank(userId))
                .map(i -> i >= 0 ? i + 1 : i);
    }
}
