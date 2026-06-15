package com.example.webflux.controller;

import com.example.webflux.controller.dto.ServerExceptionResponse;
import com.example.webflux.controller.dto.WaitingQueueResponse;
import com.example.webflux.service.WaitingQueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static com.example.webflux.exception.QueueErrorCode.ALREADY_RESISTER_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@WebFluxTest(WaitingQueueController.class)
class WaitingQueueControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private WaitingQueueService waitingQueueService;


    @Test
    @DisplayName("대기열 큐에 유저를 등록한다.")
    void enqueueWaitingQueue() {
        Long givenUserId = 1L;
        Long givenRank = 99L;

        when(waitingQueueService.enqueueWaitingQueue(givenUserId))
                .thenReturn(Mono.just(givenRank));

        webTestClient.post()
                .uri("/api/v1/waiting/queue?userId=" + givenUserId)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(WaitingQueueResponse.class)
                .value(response -> {
                    assertThat(response.getRank()).isEqualTo(givenRank);
                });
    }

    @Test
    @DisplayName("이미 등록된 사용자인 경우 예외를 응답한다.")
    void enqueueWaitingQueueThrowException() {
        when(waitingQueueService.enqueueWaitingQueue(99L))
                .thenThrow(ALREADY_RESISTER_USER.build());

        webTestClient.post()
                .uri("/api/v1/waiting/queue?userId=99")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ServerExceptionResponse.class)
                .value(
                        response -> {
                            assertThat(response.code()).isEqualTo(ALREADY_RESISTER_USER.getCode());
                            assertThat(response.reason()).isEqualTo(ALREADY_RESISTER_USER.getReason());
                        });
    }
}