package vn.it4409.g16.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Đồng hồ "tua" được, để test khoá tài khoản / thời gian ân hạn mà không phải sleep thật. */
public class MutableClock extends Clock {
    private volatile Instant now = Instant.now();

    public void advance(Duration d) { now = now.plus(d); }

    @Override public Instant instant() { return now; }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
}
