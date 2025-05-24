package org.example.logfile.id;

import org.example.clock.Clock;

public class TimestampBasedFileIdGenerator {
    private Clock clock;

    public TimestampBasedFileIdGenerator(Clock clock) {
        this.clock = clock;
    }

    public long next() {
        return clock.now();
    }
}
