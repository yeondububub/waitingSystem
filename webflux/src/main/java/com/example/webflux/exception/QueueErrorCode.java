package com.example.webflux.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QueueErrorCode {

    ALREADY_RESISTER_USER(HttpStatus.CONFLICT, "WQ-0001", "Already registered in queue");

    private final HttpStatus status;
    private final String code;
    private final String reason;

    public WaitingQueueException build() {
        return new WaitingQueueException(status, code, reason);
    }

    public WaitingQueueException build(Object... args) {
        return new WaitingQueueException(status, code, reason.formatted(args));
    }
}
