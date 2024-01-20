package com.preservinc.production.djr.auth.accesskey;

import lombok.Getter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
public enum KeyDuration {
    SHORT(Duration.of(10, ChronoUnit.MINUTES)),
    LONG(Duration.of(24, ChronoUnit.HOURS)),
    INFINITE(Duration.of(Long.MAX_VALUE, ChronoUnit.FOREVER));

    private final Duration duration;

    KeyDuration(Duration duration) {
        this.duration = duration;
    }
}
