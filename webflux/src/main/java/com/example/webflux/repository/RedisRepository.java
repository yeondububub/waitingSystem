package com.example.webflux.repository;

import org.springframework.data.redis.core.ZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RedisRepository {

    Mono<Boolean> addZSetIfAbsent(String queue, Long userId, Long timeStamp);
    Mono<Long> addZSetIfAbsentAndRank(String queue, Long userId, Long timestamp);
    Mono<Long> zRank(String queue, Long userId);
    Flux<ZSetOperations.TypedTuple<String>> popMin(String queue, Long count);
    Flux<String> scan(String pattern, Long count);
    Mono<Long> removeRangeByScore(String queue, Double min, Double max);
}
