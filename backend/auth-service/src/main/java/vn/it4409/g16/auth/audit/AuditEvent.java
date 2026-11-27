package vn.it4409.g16.auth.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_log")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 40)
    private String event;

    @Column(length = 254)
    private String subject;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {}

    AuditEvent(UUID userId, String event, String subject, String ip, String userAgent, boolean success, Instant at) {
        this.userId = userId;
        this.event = event;
        this.subject = subject;
        this.ip = ip;
        this.userAgent = userAgent;
        this.success = success;
        this.occurredAt = at;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getEvent() { return event; }
    public String getSubject() { return subject; }
    public boolean isSuccess() { return success; }
    public Instant getOccurredAt() { return occurredAt; }
}
