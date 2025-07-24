package app.brecks.auth.accesskey;

import lombok.Getter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
public enum KeyDuration {
    SHORT(Duration.of(10, ChronoUnit.MINUTES)),
    LONG(Duration.of(24, ChronoUnit.HOURS)),
    EXTRA_LONG(Duration.of(30, ChronoUnit.DAYS));

    private final Duration duration;

    KeyDuration(Duration duration) {
        this.duration = duration;
    }
}
