package vn.it4409.g16.user.address;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import vn.it4409.g16.user.crypto.EncryptedStringConverter;

@Entity
@Table(name = "addresses")
public class Address {

    /** UUID ngẫu nhiên: không đoán được id địa chỉ của người khác bằng cách +1. */
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String receiver;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_enc", nullable = false)
    private String phone;

    @Column(nullable = false, length = 200)
    private String line1;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Address() {}

    static Address create(UUID userId, AddressData d, boolean isDefault, Instant now) {
        Address a = new Address();
        a.id = UUID.randomUUID();
        a.userId = userId;
        a.apply(d, now);
        a.isDefault = isDefault;
        a.createdAt = now;
        return a;
    }

    void apply(AddressData d, Instant now) {
        this.receiver = d.receiver();
        this.phone = d.phone();
        this.line1 = d.line1();
        this.ward = d.ward();
        this.district = d.district();
        this.province = d.province();
        this.updatedAt = now;
    }

    void setDefault(boolean value, Instant now) {
        this.isDefault = value;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getReceiver() { return receiver; }
    public String getPhone() { return phone; }
    public String getLine1() { return line1; }
    public String getWard() { return ward; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    public boolean isDefault() { return isDefault; }
    public Instant getCreatedAt() { return createdAt; }
}
