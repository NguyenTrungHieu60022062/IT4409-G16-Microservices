package vn.it4409.g16.user.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import vn.it4409.g16.user.crypto.EncryptedStringConverter;

@Entity
@Table(name = "profiles")
public class Profile {

    /** = "sub" trong JWT. Không tự sinh: hồ sơ gắn 1-1 với tài khoản bên Auth Service. */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_enc")
    private String phone;

    private LocalDate dob;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Profile() {}

    static Profile empty(UUID userId, Instant now) {
        Profile p = new Profile();
        p.userId = userId;
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    /** Chỉ những trường này được sửa — userId, createdAt, version không bao giờ lấy từ client. */
    void update(String fullName, String phone, LocalDate dob, Instant now) {
        this.fullName = fullName;
        this.phone = phone;
        this.dob = dob;
        this.updatedAt = now;
    }

    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public LocalDate getDob() { return dob; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
