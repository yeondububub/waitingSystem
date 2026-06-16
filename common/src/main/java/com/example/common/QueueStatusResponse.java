package com.example.common;

public record QueueStatusResponse (
        boolean accessible,
        long rank
) { }
