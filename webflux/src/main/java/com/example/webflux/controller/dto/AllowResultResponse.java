package com.example.webflux.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AllowResultResponse {
    private Long requestCount;
    private Long allowedCount;
}
