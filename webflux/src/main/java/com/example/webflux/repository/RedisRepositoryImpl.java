package com.example.webflux.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReactiveZSetCommands;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    public final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public Mono<Boolean> addZSetIfAbsent(String queue, Long userId, Long timeStamp) {
        ReactiveZSetCommands.ZAddCommand zAddCommand = ReactiveZSetCommands.ZAddCommand.tuple(
                        Tuple.of(userId.toString().getBytes(), timeStamp.doubleValue())
                )
                .nx()
                .to(ByteBuffer.wrap(queue.getBytes()));

        return reactiveRedisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .zSetCommands()
                .zAdd(Mono.just(zAddCommand))
                .next()
                .map(response -> response.getOutput() != null && response.getOutput().intValue() == 1);
    }

    @Override
    public Mono<Long> addZSetIfAbsentAndRank(String queue, Long userId, Long timestamp) {
        String script = """
        local added = redis.call('ZADD', KEYS[1], 'NX', ARGV[2], ARGV[1])
        return redis.call('ZRANK', KEYS[1], ARGV[1])
        """;

        List<String> keys = Collections.singletonList(queue);
        List<String> args = List.of(userId.toString(), timestamp.toString());

        return reactiveRedisTemplate.execute(RedisScript.of(script, Long.class), keys, args)
                .next();
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

    @Override
    public Flux<String> scan(String pattern, Long count) {
        return reactiveRedisTemplate.scan(
                ScanOptions.scanOptions()
                .match(pattern)
                .count(count)
                .build()
        );
    }

    @Override
    public Mono<Long> removeRangeByScore(String queue, Double min, Double max) {
        return reactiveRedisTemplate.opsForZSet()
                .removeRangeByScore(queue, org.springframework.data.domain.Range.closed(min, max));
    }
}
