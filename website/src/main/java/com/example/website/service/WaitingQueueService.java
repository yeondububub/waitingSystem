package com.example.website.service;

import com.example.common.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final WebClient webClient;

    public QueueStatusResponse accessibleCheck(Long userId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/queue/checked").queryParam("userId", userId).build())
                .retrieve()
                .bodyToMono(QueueStatusResponse.class)
                .block();
    }
}
