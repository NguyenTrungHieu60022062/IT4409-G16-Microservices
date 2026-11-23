package vn.it4409.g16.auth.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Danh mục quyền, dạng "tài_nguyên:hành_động" (vd "product:write"). Chỉ đọc, sửa bằng migration. */
@Entity
@Immutable
@Table(name = "permissions")
public class Permission {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false)
    private String description;

    protected Permission() {}

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
