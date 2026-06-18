package com.example.webflux.controller;

import com.example.common.QueueStatusResponse;
import com.example.webflux.controller.dto.AllowResultResponse;
import com.example.webflux.controller.dto.AllowedResponse;
import com.example.webflux.controller.dto.WaitingQueueResponse;
import com.example.webflux.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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

    @GetMapping("/waiting/queue/allowed")
    public Mono<AllowedResponse> isAllowed(@RequestParam(name = "userId") Long userId) {
        return queueService.isAllowed(userId)
                .map(AllowedResponse::new);
    }

    @GetMapping("/queue/checked")
    public Mono<QueueStatusResponse> checked(@RequestParam("userId") Long userId) {
        return queueService.checked(userId)
                .map(rank -> new QueueStatusResponse(rank == 0, rank));
    }
}
