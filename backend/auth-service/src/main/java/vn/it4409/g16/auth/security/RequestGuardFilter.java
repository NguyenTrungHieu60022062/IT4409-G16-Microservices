package vn.it4409.g16.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.it4409.g16.auth.config.AuthProperties;

/**
 * Hai kiểm tra rẻ, chạy trước mọi thứ khác:
 *
 * <p>1. <b>Kiểm Origin (chống CSRF)</b>. CORS chỉ ngăn trang lạ ĐỌC phản hồi, KHÔNG ngăn request
 * được gửi đi. Một trang độc có thể tự submit form POST tới /api/auth/logout hoặc /api/auth/login
 * (login CSRF: đăng nhập nạn nhân vào tài khoản của kẻ tấn công). Trình duyệt luôn gắn header
 * Origin vào request POST nên ta từ chối mọi Origin không nằm trong danh sách cho phép.
 * Request không có Origin (curl, Postman, service khác) không phải từ trình duyệt -> không có
 * cookie của nạn nhân -> không có rủi ro CSRF.
 *
 * <p>2. <b>Giới hạn kích thước body</b>: chống gửi JSON khổng lồ làm tốn RAM/CPU.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestGuardFilter extends OncePerRequestFilter {

    static final long MAX_BODY_BYTES = 16 * 1024;
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<String> allowedOrigins;

    public RequestGuardFilter(AuthProperties props) {
        this.allowedOrigins = Set.copyOf(props.allowedOrigins());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!SAFE_METHODS.contains(req.getMethod())) {
            String origin = req.getHeader("Origin");
            if (origin != null && !allowedOrigins.contains(origin)) {
                reject(res, HttpServletResponse.SC_FORBIDDEN, "ORIGIN_NOT_ALLOWED", "Nguồn gửi yêu cầu không được phép.");
                return;
            }
            if (req.getContentLengthLong() > MAX_BODY_BYTES) {
                reject(res, 413, "PAYLOAD_TOO_LARGE", "Dữ liệu gửi lên quá lớn.");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private static void reject(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"details\":[]}");
    }
}
