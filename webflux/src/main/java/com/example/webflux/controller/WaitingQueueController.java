package com.example.webflux.controller;

import com.example.webflux.controller.dto.WaitingQueueResponse;
import com.example.webflux.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WaitingQueueController {

    public final WaitingQueueService waitingQueueService;

    @PostMapping("/waiting/queue")
    public Mono<WaitingQueueResponse> enqueueWaitingQueue(@RequestParam(name = "userId") Long userId) {
        return waitingQueueService.enqueueWaitingQueue(userId)
                .map(WaitingQueueResponse::new);
    }
}
