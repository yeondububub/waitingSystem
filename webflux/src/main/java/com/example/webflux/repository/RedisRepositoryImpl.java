package com.example.webflux.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    public final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public Mono<Boolean> addZSet(String queue, Long userId, Long timeStamp) {
        return reactiveRedisTemplate.opsForZSet()
                .add(queue, userId.toString(), timeStamp);
    }

    @Override
    public Mono<Long> zRank(String queue, Long userId) {
        return reactiveRedisTemplate.opsForZSet()
                .rank(queue, userId.toString());
    }

    @Override
    public Flux<ZSetOperations.TypedTuple<String>> popMin(String queue, Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(queue, count);
    }
}
