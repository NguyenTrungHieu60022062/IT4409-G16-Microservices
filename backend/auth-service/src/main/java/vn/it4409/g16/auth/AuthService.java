package vn.it4409.g16.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.auth.audit.AuditService;
import vn.it4409.g16.auth.security.ClientInfo;
import vn.it4409.g16.auth.security.Emails;
import vn.it4409.g16.auth.security.LoginAttemptService;
import vn.it4409.g16.auth.security.PasswordPolicy;
import vn.it4409.g16.auth.token.AccessTokenService;
import vn.it4409.g16.auth.token.AccessTokenService.AccessToken;
import vn.it4409.g16.auth.token.RefreshToken;
import vn.it4409.g16.auth.token.RefreshTokenService;
import vn.it4409.g16.auth.token.RefreshTokenService.Issued;
import vn.it4409.g16.auth.user.UserAccount;
import vn.it4409.g16.auth.user.UserRepository;
import vn.it4409.g16.auth.web.ApiException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Kết quả đăng nhập/refresh: access token cho body + refresh token cho cookie. */
    public record Session(AccessToken accessToken, Issued refresh) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final LoginAttemptService attempts;
    private final RefreshTokenService refreshTokens;
    private final AccessTokenService accessTokens;
    private final AuditService audit;
    private final Clock clock;
    /** Hash "giả" để so khi email không tồn tại — giữ thời gian phản hồi như nhau. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, PasswordPolicy policy,
                       LoginAttemptService attempts, RefreshTokenService refreshTokens,
                       AccessTokenService accessTokens, AuditService audit, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.policy = policy;
        this.attempts = attempts;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = encoder.encode("dummy-password-for-constant-time-" + UUID.randomUUID());
    }

    // ---------------------------------------------------------------- đăng ký

    /**
     * Luôn trả về CÙNG một kết quả dù email đã tồn tại hay chưa (chống dò tài khoản). Nếu trả "Email
     * đã được sử dụng", kẻ tấn công chỉ cần gọi API với danh sách email để biết ai là khách hàng
     * của shop -> dùng cho phishing hoặc credential stuffing.
     * Ở hệ thống thật: email đã tồn tại -> gửi mail "Bạn đã có tài khoản, quên mật khẩu?" cho chủ email.
     */
    public void register(String rawEmail, String rawPassword, ClientInfo client) {
        String email = Emails.normalize(rawEmail);
        String password = PasswordPolicy.normalize(rawPassword);
        policy.validate(password, email); // không phụ thuộc email có tồn tại hay không -> không lộ gì

        // Băm TRƯỚC khi kiểm tra tồn tại: cả 2 nhánh đều tốn ~cùng thời gian (chống timing attack).
        String hash = encoder.encode(password);
        if (users.existsByEmail(email)) {
            audit.record("REGISTER_DUPLICATE", null, Emails.mask(email), client, false);
            log.info("[DEV] Email {} đã có tài khoản — hệ thống thật sẽ gửi email thông báo cho chủ tài khoản.",
                    Emails.mask(email));
            return;
        }
        try {
            UserAccount user = users.saveAndFlush(UserAccount.newCustomer(email, hash, clock.instant()));
            audit.record("REGISTER", user.getId(), Emails.mask(email), client, true);
        } catch (DataIntegrityViolationException race) {
            // 2 request đăng ký cùng email chạy song song: UNIQUE constraint chặn bản thứ 2.
            audit.record("REGISTER_DUPLICATE", null, Emails.mask(email), client, false);
        }
    }

    // ---------------------------------------------------------------- đăng nhập

    public Session login(String rawEmail, String rawPassword, boolean rememberMe, ClientInfo client) {
        String email = Emails.normalize(rawEmail);
        String password = PasswordPolicy.normalize(rawPassword);
        attempts.checkNotLocked(email); // đang bị khoá -> 429, kể cả khi lần này nhập đúng

        UserAccount user = users.findByEmail(email).orElse(null);
        boolean ok;
        if (user == null) {
            encoder.matches(password, dummyHash); // tốn thời gian như nhánh thật, kết quả bỏ đi
            ok = false;
        } else {
            ok = encoder.matches(password, user.getPasswordHash()) && user.isActive();
        }

        if (!ok) {
            attempts.recordFailure(email);
            audit.record("LOGIN_FAILURE", user == null ? null : user.getId(), Emails.mask(email), client, false);
            // Một thông điệp chung cho: sai email, sai mật khẩu, tài khoản bị khoá bởi admin.
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng.");
        }

        attempts.reset(email);
        upgradeHashIfNeeded(user, password);
        Issued refresh = refreshTokens.startSession(user.getId(), rememberMe, client);
        audit.record("LOGIN_SUCCESS", user.getId(), Emails.mask(email), client, true);
        return new Session(accessTokens.issue(user), refresh);
    }

    /** Tham số Argon2 tăng lên theo thời gian -> băm lại hash cũ ngay khi biết mật khẩu gốc (lúc đăng nhập). */
    private void upgradeHashIfNeeded(UserAccount user, String password) {
        if (encoder.upgradeEncoding(user.getPasswordHash())) {
            user.rehash(encoder.encode(password));
            users.save(user);
        }
    }

    // ---------------------------------------------------------------- refresh / logout

    public Session refresh(String rawRefreshToken, ClientInfo client) {
        Issued next = refreshTokens.rotate(rawRefreshToken, client);
        UserAccount user = users.findById(next.token().getUserId()).orElse(null);
        if (user == null || !user.isActive()) {
            refreshTokens.revokeFamily(next.token().getFamilyId(), "USER_DISABLED");
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }
        return new Session(accessTokens.issue(user), next);
    }

    public void logout(String rawRefreshToken, ClientInfo client) {
        RefreshToken t = refreshTokens.revokeByRawToken(rawRefreshToken, "LOGOUT");
        if (t != null) audit.record("LOGOUT", t.getUserId(), null, client, true);
    }

    public void logoutAll(Jwt jwt, ClientInfo client) {
        UserAccount user = requireActiveUser(jwt);
        refreshTokens.revokeAllOfUser(user.getId(), "LOGOUT_ALL");
        audit.record("LOGOUT_ALL", user.getId(), null, client, true);
    }

    // ---------------------------------------------------------------- tài khoản

    /** Tài khoản đang gọi, sau khi kiểm: còn hoạt động, token không cũ hơn lần đổi mật khẩu/quyền. */
    public UserAccount me(Jwt jwt) {
        return requireActiveUser(jwt);
    }

    @Transactional
    public void changePassword(Jwt jwt, String currentPassword, String newPassword, ClientInfo client) {
        UserAccount user = requireActiveUser(jwt);
        String key = "change-password:" + user.getId();
        attempts.checkNotLocked(key); // ai đó cầm được access token cũng không đoán mật khẩu cũ mãi được

        String current = PasswordPolicy.normalize(currentPassword);
        if (!encoder.matches(current, user.getPasswordHash())) {
            attempts.recordFailure(key);
            audit.record("PASSWORD_CHANGE_FAILURE", user.getId(), null, client, false);
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "Mật khẩu hiện tại không đúng.", List.of());
        }
        String next = PasswordPolicy.normalize(newPassword);
        if (next.equals(current)) {
            throw ApiException.badRequest("SAME_PASSWORD", "Mật khẩu mới phải khác mật khẩu hiện tại.", List.of());
        }
        policy.validate(next, user.getEmail());

        attempts.reset(key);
        user.changePassword(encoder.encode(next), clock.instant());
        users.save(user);
        // Mật khẩu cũ có thể đã lộ -> mọi phiên đang mở (kể cả của kẻ tấn công) phải bị đá ra.
        refreshTokens.revokeAllOfUser(user.getId(), "PASSWORD_CHANGED");
        audit.record("PASSWORD_CHANGED", user.getId(), null, client, true);
    }

    public List<RefreshToken> sessions(Jwt jwt) {
        return refreshTokens.activeSessions(requireActiveUser(jwt).getId());
    }

    public void revokeSession(Jwt jwt, UUID familyId, ClientInfo client) {
        UserAccount user = requireActiveUser(jwt);
        // Điều kiện userId nằm ngay trong câu UPDATE. Không thuộc về mình -> 404 (không phải 403),
        // để không xác nhận cho kẻ dò rằng id đó có tồn tại.
        if (!refreshTokens.revokeFamilyOfUser(familyId, user.getId(), "SESSION_REVOKED")) {
            throw ApiException.of(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên đăng nhập.");
        }
        audit.record("SESSION_REVOKED", user.getId(), null, client, true);
    }

    /**
     * JWT hợp lệ về chữ ký chưa đủ: tài khoản có thể đã bị vô hiệu hoá, hoặc mật khẩu đã đổi SAU khi
     * token được cấp (token cũ có thể nằm trong tay kẻ tấn công). Chính Auth Service kiểm được điều
     * này vì nó có DB; các service khác chỉ dựa vào thời hạn 15 phút của token.
     */
    private UserAccount requireActiveUser(Jwt jwt) {
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Phiên đăng nhập không hợp lệ.");
        }
        UserAccount user = users.findById(id).orElse(null);
        if (user == null || !user.isActive()) {
            throw ApiException.unauthorized("INVALID_TOKEN", "Phiên đăng nhập không hợp lệ.");
        }
        Instant issuedAt = jwt.getIssuedAt();
        if (issuedAt == null || issuedAt.isBefore(user.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS))
                // Bị đổi role / khoá sau khi token được cấp: quyền trong token đã cũ -> không tin nữa.
                || (user.getAuthzChangedAt() != null
                    && issuedAt.isBefore(user.getAuthzChangedAt().truncatedTo(ChronoUnit.SECONDS)))) {
            throw ApiException.unauthorized("TOKEN_REVOKED", "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.");
        }
        return user;
    }
}
