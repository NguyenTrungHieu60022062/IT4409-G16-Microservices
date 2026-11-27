package vn.it4409.g16.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.auth.audit.AuditService;
import vn.it4409.g16.auth.config.AuthProperties;
import vn.it4409.g16.auth.security.ClientInfo;
import vn.it4409.g16.auth.web.ApiException;

/**
 * Refresh token = chuỗi ngẫu nhiên 256 bit (KHÔNG phải JWT), gửi qua cookie HttpOnly.
 *
 * <p><b>Xoay vòng (rotation)</b>: mỗi lần /refresh, token cũ bị vô hiệu và cấp token mới cùng
 * family. <b>Phát hiện tái sử dụng</b>: nếu token CŨ (đã xoay) lại được gửi lên, nghĩa là có 2 bên
 * đang giữ cùng một token — người dùng thật và kẻ đã đánh cắp nó. Ta không biết bên nào là thật,
 * nên thu hồi CẢ family: kẻ trộm mất quyền, người dùng thật chỉ phải đăng nhập lại.
 */
@Service
public class RefreshTokenService {

    public static final String REASON_ROTATED = "ROTATED";

    public record Issued(String rawToken, RefreshToken token) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final AuditService audit;
    private final AuthProperties props;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repo, AuditService audit, AuthProperties props, Clock clock) {
        this.repo = repo;
        this.audit = audit;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public Issued startSession(UUID userId, boolean rememberMe, ClientInfo client) {
        Instant now = clock.instant();
        // Hạn TUYỆT ĐỐI của phiên được chốt ngay lúc đăng nhập. Token kế tiếp thừa hưởng mốc này,
        // nên kẻ giữ được token cũng không thể "gia hạn mãi mãi" bằng cách refresh liên tục.
        Instant expiresAt = now.plus(rememberMe ? props.rememberMeTtl() : props.sessionTtl());
        return issue(UUID.randomUUID(), userId, UUID.randomUUID(), rememberMe, now, now, expiresAt, client);
    }

    @Transactional(noRollbackFor = ApiException.class) // thu hồi family rồi mới ném lỗi -> không được rollback
    public Issued rotate(String rawToken, ClientInfo client) {
        if (rawToken == null || rawToken.isBlank()) throw invalid();
        RefreshToken current = repo.findByTokenHash(hash(rawToken)).orElseThrow(RefreshTokenService::invalid);
        Instant now = clock.instant();

        if (current.getRevokedAt() != null) {
            if (REASON_ROTATED.equals(current.getRevokeReason())) {
                if (now.isBefore(current.getRevokedAt().plus(props.refreshReuseGrace()))) {
                    // Vừa xoay vài giây trước: thường là 2 tab cùng refresh. Báo client thử lại
                    // (cookie lúc này đã là token mới) thay vì đăng xuất người dùng oan.
                    throw concurrent();
                }
                repo.revokeFamily(current.getFamilyId(), "REUSE_DETECTED", now);
                audit.record("REFRESH_REUSE_DETECTED", current.getUserId(), null, client, false);
            }
            throw invalid();
        }
        if (!now.isBefore(current.getExpiresAt())) throw invalid();

        UUID nextId = UUID.randomUUID();
        if (repo.markRotated(current.getId(), nextId, now) == 0) {
            throw concurrent(); // request song song khác đã xoay token này trước ta
        }
        return issue(nextId, current.getUserId(), current.getFamilyId(), current.isRememberMe(),
                current.getSessionStartedAt(), now, current.getExpiresAt(), client);
    }

    /** Trả về token (nếu tìm thấy) để biết đang đăng xuất phiên của ai. */
    @Transactional
    public RefreshToken revokeByRawToken(String rawToken, String reason) {
        if (rawToken == null || rawToken.isBlank()) return null;
        return repo.findByTokenHash(hash(rawToken))
                .map(t -> {
                    repo.revokeFamily(t.getFamilyId(), reason, clock.instant());
                    return t;
                })
                .orElse(null);
    }

    @Transactional
    public void revokeFamily(UUID familyId, String reason) {
        repo.revokeFamily(familyId, reason, clock.instant());
    }

    @Transactional
    public boolean revokeFamilyOfUser(UUID familyId, UUID userId, String reason) {
        return repo.revokeFamilyOfUser(familyId, userId, reason, clock.instant()) > 0;
    }

    @Transactional
    public void revokeAllOfUser(UUID userId, String reason) {
        repo.revokeAllOfUser(userId, reason, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> activeSessions(UUID userId) {
        return repo.findActiveByUser(userId, clock.instant());
    }

    /** familyId của token trong cookie hiện tại (để đánh dấu "thiết bị này"), không kiểm hạn. */
    @Transactional(readOnly = true)
    public UUID familyOf(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        return repo.findByTokenHash(hash(rawToken)).map(RefreshToken::getFamilyId).orElse(null);
    }

    private Issued issue(UUID id, UUID userId, UUID familyId, boolean rememberMe, Instant sessionStartedAt,
                         Instant now, Instant expiresAt, ClientInfo client) {
        byte[] bytes = new byte[32]; // 256 bit từ CSPRNG -> không thể đoán hay brute force
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = RefreshToken.create(id, userId, familyId, hash(raw), rememberMe,
                sessionStartedAt, now, expiresAt, client.ip(), client.userAgent());
        repo.save(token);
        return new Issued(raw, token);
    }

    /**
     * Chỉ lưu SHA-256 của token. Khác với mật khẩu, KHÔNG cần Argon2 ở đây: token có 256 bit ngẫu
     * nhiên nên không thể brute force ngược từ hash; hash nhanh lại cho phép tra cứu theo index.
     */
    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException invalid() {
        return ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }

    private static ApiException concurrent() {
        return ApiException.of(HttpStatus.CONFLICT, "REFRESH_IN_PROGRESS", "Phiên đang được làm mới, vui lòng thử lại.");
    }
}
