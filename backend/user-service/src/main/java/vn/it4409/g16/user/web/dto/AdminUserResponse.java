package vn.it4409.g16.user.web.dto;

import java.time.Instant;
import java.util.UUID;
import vn.it4409.g16.user.profile.Profile;

/**
 * Admin chỉ thấy những gì cần cho việc quản trị (nguyên tắc tối thiểu dữ liệu — data minimization):
 * số điện thoại bị che, không có ngày sinh, không có địa chỉ.
 */
public record AdminUserResponse(UUID userId, String fullName, String phoneMasked, Instant createdAt) {
    public static AdminUserResponse of(Profile p) {
        return new AdminUserResponse(p.getUserId(), p.getFullName(), Inputs.maskPhone(p.getPhone()), p.getCreatedAt());
    }
}
