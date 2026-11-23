package vn.it4409.g16.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.it4409.g16.auth.config.AuthProperties;
import vn.it4409.g16.auth.web.ApiException;

/**
 * Khoá tạm thời theo TÀI KHOẢN (khác RateLimiter khoá theo IP).
 *
 * <ul>
 *   <li>Sai {@code maxFailures} lần trong {@code window} -> khoá {@code baseLock}; mỗi lần bị khoá
 *       tiếp theo thời gian khoá nhân đôi, tối đa {@code maxLock}.</li>
 *   <li>Áp dụng cho CẢ email không tồn tại: nếu chỉ email thật mới bị khoá thì kẻ tấn công nhìn
 *       phản hồi 429 là biết email đó có tài khoản (user enumeration).</li>
 *   <li>Khoá có thời hạn ngắn thay vì khoá vĩnh viễn: nếu khoá vĩnh viễn, kẻ tấn công chỉ cần cố
 *       tình nhập sai 5 lần là khoá được tài khoản của bất kỳ ai (DoS).</li>
 *   <li>Brute force từ nhiều IP vào một tài khoản -> bị chặn ở đây; một IP thử nhiều tài khoản
 *       (password spraying) -> bị RateLimiter chặn. Hai lớp bổ sung cho nhau.</li>
 * </ul>
 * Lưu trong RAM (mất khi restart) — bản nhiều instance nên chuyển sang Redis.
 */
@Component
public class LoginAttemptService {

    private record State(int failures, long windowStart, long lockedUntil, int lockCount, long lastSeen) {}

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AuthProperties.Lockout cfg;

    public LoginAttemptService(Clock clock, AuthProperties props) {
        this.clock = clock;
        this.cfg = props.lockout();
    }

    public void checkNotLocked(String key) {
        State s = states.get(key);
        long now = clock.millis();
        if (s != null && s.lockedUntil() > now) {
            throw ApiException.tooManyRequests(Math.max(1, (s.lockedUntil() - now + 999) / 1000));
        }
    }

    public void recordFailure(String key) {
        long now = clock.millis();
        states.compute(key, (k, old) -> {
            State s = old;
            if (s == null) {
                s = new State(0, now, 0, 0, now);
            } else if (now - s.windowStart() > cfg.window().toMillis()) {
                s = new State(0, now, s.lockedUntil(), s.lockCount(), now); // cửa sổ mới, giữ lịch sử khoá
            }
            int failures = s.failures() + 1;
            if (failures >= cfg.maxFailures()) {
                long lockMillis = Math.min(
                        cfg.baseLock().toMillis() << Math.min(s.lockCount(), 20),
                        cfg.maxLock().toMillis());
                return new State(0, now, now + lockMillis, s.lockCount() + 1, now);
            }
            return new State(failures, s.windowStart(), s.lockedUntil(), s.lockCount(), now);
        });
    }

    public void reset(String key) {
        states.remove(key);
    }

    @Scheduled(fixedRate = 300_000)
    void evictStale() {
        long now = clock.millis();
        long keep = Duration.ofHours(24).toMillis();
        states.entrySet().removeIf(e -> e.getValue().lockedUntil() < now && now - e.getValue().lastSeen() > keep);
    }
}
