package com.example.webflux.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class WaitingQueueException extends RuntimeException {
    private HttpStatus httpStatus;
    private String code;
    private String reason;
}
