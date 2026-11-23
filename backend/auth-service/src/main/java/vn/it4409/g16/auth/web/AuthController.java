package vn.it4409.g16.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.it4409.g16.auth.AuthService;
import vn.it4409.g16.auth.AuthService.Session;
import vn.it4409.g16.auth.config.AuthProperties;
import vn.it4409.g16.auth.rbac.Role;
import vn.it4409.g16.auth.rbac.RoleRepository;
import vn.it4409.g16.auth.security.ClientInfo;
import vn.it4409.g16.auth.security.RateLimiter;
import vn.it4409.g16.auth.token.RefreshToken;
import vn.it4409.g16.auth.token.RefreshTokenService;
import vn.it4409.g16.auth.user.UserAccount;
import vn.it4409.g16.auth.web.dto.ChangePasswordRequest;
import vn.it4409.g16.auth.web.dto.ErrorResponse;
import vn.it4409.g16.auth.web.dto.LoginRequest;
import vn.it4409.g16.auth.web.dto.MeResponse;
import vn.it4409.g16.auth.web.dto.MessageResponse;
import vn.it4409.g16.auth.web.dto.RegisterRequest;
import vn.it4409.g16.auth.web.dto.SessionResponse;
import vn.it4409.g16.auth.web.dto.TokenResponse;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    public static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService auth;
    private final RefreshTokenService refreshTokens;
    private final RateLimiter rateLimiter;
    private final AuthProperties props;
    private final Clock clock;
    private final RoleRepository roles;

    public AuthController(AuthService auth, RefreshTokenService refreshTokens, RateLimiter rateLimiter,
                          AuthProperties props, Clock clock, RoleRepository roles) {
        this.auth = auth;
        this.refreshTokens = refreshTokens;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.clock = clock;
        this.roles = roles;
    }

    // consumes=JSON: form HTML (text/plain, x-www-form-urlencoded) do trang lạ tự submit bị 415 ngay.
    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest req) {
        ClientInfo client = ClientInfo.from(req);
        rateLimiter.check("register", client.ip(), props.rateLimit().registerPerHour(), Duration.ofHours(1));
        auth.register(body.email(), body.password(), client);
        // 202 + thông điệp chung: không tiết lộ email đã có tài khoản hay chưa.
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MessageResponse(
                "Nếu email hợp lệ và chưa được sử dụng, tài khoản đã được tạo. Hãy đăng nhập để tiếp tục."));
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest req) {
        ClientInfo client = ClientInfo.from(req);
        rateLimiter.check("login", client.ip(), props.rateLimit().loginPerMinute(), Duration.ofMinutes(1));
        return withSession(auth.login(body.email(), body.password(), body.rememberMe(), client));
    }

    /** Không cần Bearer: danh tính lấy từ cookie HttpOnly (JavaScript không đọc được cookie này). */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
                                     HttpServletRequest req) {
        ClientInfo client = ClientInfo.from(req);
        rateLimiter.check("refresh", client.ip(), props.rateLimit().refreshPerMinute(), Duration.ofMinutes(1));
        try {
            return withSession(auth.refresh(cookie, client));
        } catch (ApiException e) {
            if (e.status() == HttpStatus.UNAUTHORIZED) {
                // Token hỏng/hết hạn: xoá luôn cookie để trình duyệt không gửi lại mãi.
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                        .body(new ErrorResponse(e.code(), e.getMessage(), e.details()));
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String cookie,
                                       HttpServletRequest req) {
        auth.logout(cookie, ClientInfo.from(req));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt, HttpServletRequest req) {
        auth.logoutAll(jwt, ClientInfo.from(req));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserAccount u = auth.me(jwt);
        // permissions chỉ để frontend ẩn/hiện menu. Quyết định cho/không cho vẫn nằm ở server.
        List<String> perms = roles.findById(u.getRole()).map(Role::sortedPermissions).orElse(List.of());
        return new MeResponse(u.getId(), u.getEmail(), u.getRole(), perms, u.getCreatedAt(), u.getPasswordChangedAt());
    }

    @PostMapping(path = "/change-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest body,
                                               HttpServletRequest req) {
        auth.changePassword(jwt, body.currentPassword(), body.newPassword(), ClientInfo.from(req));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@AuthenticationPrincipal Jwt jwt,
                                          @CookieValue(name = REFRESH_COOKIE, required = false) String cookie) {
        UUID currentFamily = refreshTokens.familyOf(cookie);
        return auth.sessions(jwt).stream()
                .map(t -> toResponse(t, Objects.equals(t.getFamilyId(), currentFamily)))
                .toList();
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              HttpServletRequest req) {
        auth.revokeSession(jwt, id, ClientInfo.from(req));
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ cookie

    private ResponseEntity<TokenResponse> withSession(Session s) {
        RefreshToken t = s.refresh().token();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(s.refresh().rawToken(), t).toString())
                .body(TokenResponse.bearer(s.accessToken().value(), s.accessToken().expiresInSeconds()));
    }

    /**
     * HttpOnly  : JavaScript (kể cả script XSS) không đọc được.
     * Secure    : chỉ gửi qua HTTPS (localhost được trình duyệt coi là an toàn).
     * SameSite=Strict : trình duyệt không gửi cookie khi request xuất phát từ trang khác -> chống CSRF.
     * Path=/api/auth : cookie chỉ đi kèm request tới Auth, không rò sang API của service khác.
     * Không tick "Ghi nhớ" -> cookie phiên (mất khi đóng trình duyệt); có tick -> sống tới hạn phiên.
     */
    private ResponseCookie refreshCookie(String raw, RefreshToken t) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(REFRESH_COOKIE, raw)
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Strict")
                .path(props.cookie().path());
        if (t.isRememberMe()) {
            b.maxAge(Duration.between(Instant.now(clock), t.getExpiresAt()));
        }
        return b.build();
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(props.cookie().secure())
                .sameSite("Strict")
                .path(props.cookie().path())
                .maxAge(0)
                .build();
    }

    private static SessionResponse toResponse(RefreshToken t, boolean current) {
        return new SessionResponse(t.getFamilyId(), current, t.getSessionStartedAt(), t.getCreatedAt(),
                t.getExpiresAt(), t.getCreatedIp(), t.getUserAgent(), t.isRememberMe());
    }
}
