package vn.it4409.g16.auth.rbac;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.Immutable;

/**
 * Một role = một tập permission (bảng role_permissions).
 * {@code @Immutable}: ứng dụng chỉ ĐỌC role; muốn thêm/bớt quyền phải viết migration Flyway mới
 * (qua code review), không có đường nào để sửa quyền lúc chạy.
 */
@Entity
@Immutable
@Table(name = "roles")
public class Role {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";

    @Id
    @Column(length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_code"))
    @Column(name = "permission_code", length = 50)
    private Set<String> permissions;

    protected Role() {}

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /** Sắp xếp để token và API trả kết quả ổn định (dễ test, dễ so sánh). */
    public List<String> sortedPermissions() {
        return permissions.stream().sorted().toList();
    }
}
