package com.example.webflux.controller;

import com.example.common.QueueStatusResponse;
import com.example.webflux.controller.dto.AllowResultResponse;
import com.example.webflux.controller.dto.AllowedResponse;
import com.example.webflux.controller.dto.RankNumberResponse;
import com.example.webflux.controller.dto.WaitingQueueResponse;
import com.example.webflux.service.QueueManager;
import com.example.webflux.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/waiting/queue")
    public Mono<WaitingQueueResponse> enqueueWaitingQueue(@RequestParam(name = "userId") Long userId) {
        return queueService.enqueueWaitingQueue(userId)
                .map(WaitingQueueResponse::new);
    }

    @PostMapping("/waiting/queue/allow")
    public Mono<AllowResultResponse> allow(@RequestParam(name = "count") Long count) {
        return queueService.allow(count)
                .map(allowCount -> new AllowResultResponse(count, allowCount));
    }

    @GetMapping("/queue/allowed")
    public Mono<AllowedResponse> isAllowed(
            @RequestParam("userId") Long userId,
            @RequestParam("token") String token
    ) {
        return queueService.isAllowedByToken(userId, token)
                .map(AllowedResponse::new);
    }

    @GetMapping("/waiting/queue/rank")
    public Mono<RankNumberResponse> rank(@RequestParam("userId") Long userId) {
        return queueService.rank(QueueManager.WAITING_QUEUE.getKey(), userId)
                .map(RankNumberResponse::new);
    }

    @GetMapping("/waiting/queue/checked")
    public Mono<QueueStatusResponse> checked(@RequestParam("userId") Long userId) {
        return queueService.checked(userId)
                .map(rank -> new QueueStatusResponse(rank == 0, rank));
    }

    private static final String USER_QUEUE_TOKEN = "user-queue-token";

    @GetMapping("/touch")
    public Mono<String> touch(@RequestParam("userId") Long userId, ServerWebExchange exchange) {
        log.info("touch : {}", userId);

        return Mono.defer(() -> queueService.generateToken(userId))
                .map(token -> {
                    exchange.getResponse().addCookie(
                            ResponseCookie.from(QueueController.USER_QUEUE_TOKEN, token)
                                    .maxAge(Duration.ofSeconds(300))
                                    .path("/")
                                    .build()
                    );
                    return token;
                });
    }
}
