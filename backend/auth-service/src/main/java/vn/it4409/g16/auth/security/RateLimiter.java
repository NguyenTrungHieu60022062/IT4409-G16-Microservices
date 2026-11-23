package vn.it4409.g16.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.it4409.g16.auth.web.ApiException;

/**
 * Giới hạn số request theo (hành động, IP) bằng cửa sổ cố định (fixed window).
 *
 * <p>Lưu trong RAM nên chỉ đúng khi chạy MỘT instance. Khi scale nhiều instance phải chuyển sang
 * Redis (INCR + EXPIRE) để các instance dùng chung bộ đếm — đó là bước nâng cấp trong kế hoạch.
 */
@Component
public class RateLimiter {

    private record Window(long startMillis, long lengthMillis, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Ném 429 (kèm Retry-After) nếu vượt quá {@code limit} lần trong {@code window}. */
    public void check(String action, String key, int limit, Duration window) {
        long now = clock.millis();
        long len = window.toMillis();
        Window w = windows.compute(action + ':' + key, (k, old) ->
                old == null || now - old.startMillis() >= len
                        ? new Window(now, len, 1)
                        : new Window(old.startMillis(), len, old.count() + 1));
        if (w.count() > limit) {
            long retryAfter = Math.max(1, (w.startMillis() + len - now + 999) / 1000);
            throw ApiException.tooManyRequests(retryAfter);
        }
    }

    /** Dọn các cửa sổ đã hết hạn để bộ nhớ không tăng mãi. */
    @Scheduled(fixedRate = 60_000)
    void evictExpired() {
        long now = clock.millis();
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= e.getValue().lengthMillis());
    }
}
