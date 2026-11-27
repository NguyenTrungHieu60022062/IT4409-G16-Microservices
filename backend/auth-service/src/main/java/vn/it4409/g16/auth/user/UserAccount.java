package vn.it4409.g16.auth.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.it4409.g16.auth.rbac.Role;

@Entity
@Table(name = "users")
public class UserAccount {

    public enum Status { ACTIVE, DISABLED }

    /** UUID ngẫu nhiên thay vì số tăng dần: không đoán được id kế tiếp, không lộ số lượng người dùng. */
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Mã role (khoá ngoại sang bảng roles). Mỗi tài khoản đúng MỘT role. */
    @Column(nullable = false, length = 20)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    /** Lần gần nhất role/trạng thái bị đổi. Token cấp trước mốc này không còn được Auth chấp nhận. */
    @Column(name = "authz_changed_at")
    private Instant authzChangedAt;

    protected UserAccount() {}

    /** Role luôn là CUSTOMER khi đăng ký — không bao giờ lấy role từ dữ liệu client gửi lên. */
    public static UserAccount newCustomer(String email, String passwordHash, Instant now) {
        UserAccount u = new UserAccount();
        u.id = UUID.randomUUID();
        u.email = email;
        u.passwordHash = passwordHash;
        u.role = Role.CUSTOMER;
        u.status = Status.ACTIVE;
        u.createdAt = now;
        u.passwordChangedAt = now;
        return u;
    }

    public void changePassword(String newHash, Instant now) {
        this.passwordHash = newHash;
        this.passwordChangedAt = now;
    }

    /** Băm lại với tham số mới mà KHÔNG coi là "đổi mật khẩu" (không vô hiệu hoá phiên). */
    public void rehash(String newHash) {
        this.passwordHash = newHash;
    }

    /** Chỉ gọi từ AdminService (đã kiểm quyền role:assign) hoặc AdminBootstrap. */
    public void assignRole(String roleCode, Instant now) {
        this.role = roleCode;
        this.authzChangedAt = now;
    }

    public void changeStatus(Status newStatus, Instant now) {
        this.status = newStatus;
        this.authzChangedAt = now;
    }

    public boolean isActive() { return status == Status.ACTIVE; }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public Instant getAuthzChangedAt() { return authzChangedAt; }
}
