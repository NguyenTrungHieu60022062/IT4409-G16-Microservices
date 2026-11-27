package vn.it4409.g16.auth.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Mọi token sinh ra từ cùng một lần đăng nhập có chung familyId = một phiên trên một thiết bị. */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe;

    @Column(name = "session_started_at", nullable = false)
    private Instant sessionStartedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", length = 30)
    private String revokeReason;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_ip", length = 45)
    private String createdIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    protected RefreshToken() {}

    static RefreshToken create(UUID id, UUID userId, UUID familyId, String tokenHash, boolean rememberMe,
                               Instant sessionStartedAt, Instant now, Instant expiresAt, String ip, String userAgent) {
        RefreshToken t = new RefreshToken();
        t.id = id;
        t.userId = userId;
        t.familyId = familyId;
        t.tokenHash = tokenHash;
        t.rememberMe = rememberMe;
        t.sessionStartedAt = sessionStartedAt;
        t.createdAt = now;
        t.expiresAt = expiresAt;
        t.createdIp = ip;
        t.userAgent = userAgent;
        return t;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getFamilyId() { return familyId; }
    public boolean isRememberMe() { return rememberMe; }
    public Instant getSessionStartedAt() { return sessionStartedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevokeReason() { return revokeReason; }
    public UUID getReplacedBy() { return replacedBy; }
    public String getCreatedIp() { return createdIp; }
    public String getUserAgent() { return userAgent; }
}
