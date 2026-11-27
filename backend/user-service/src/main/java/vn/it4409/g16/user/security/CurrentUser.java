package vn.it4409.g16.user.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.it4409.g16.user.web.ApiException;

/**
 * userId LUÔN lấy từ claim "sub" của JWT đã được kiểm chữ ký — không bao giờ từ URL, query hay body.
 * Đây là gốc rễ của việc chống IDOR/BOLA (OWASP API Security Top 10 #1).
 */
public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Phiên đăng nhập không hợp lệ.");
        }
    }
}
