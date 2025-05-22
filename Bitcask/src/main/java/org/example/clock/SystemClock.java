package org.example.clock;

import java.time.Instant;

public class SystemClock implements Clock {

    public static SystemClock create() {
        return new SystemClock();
    }

    @Override
    public long now() {
        return Instant.now().toEpochMilli() * 1_000_000;
    }
}
