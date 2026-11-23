package vn.it4409.g16.auth.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Toàn bộ tham số bảo mật nằm ở một chỗ (application.yml, mục "auth"). */
@ConfigurationProperties("auth")
public record AuthProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration sessionTtl,
        Duration rememberMeTtl,
        Duration refreshReuseGrace,
        List<String> allowedOrigins,
        Cookie cookie,
        Jwt jwt,
        Lockout lockout,
        RateLimit rateLimit,
        /** Email được nâng lên ADMIN khi khởi động, CHỈ khi chưa có admin nào (xem AdminBootstrap). */
        String bootstrapAdminEmail) {

    public record Cookie(String path, boolean secure) {}

    public record Jwt(String privateKeyLocation) {}

    public record Lockout(int maxFailures, Duration window, Duration baseLock, Duration maxLock) {}

    public record RateLimit(int loginPerMinute, int registerPerHour, int refreshPerMinute) {}
}
