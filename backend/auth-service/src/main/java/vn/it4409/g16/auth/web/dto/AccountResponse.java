package vn.it4409.g16.auth.web.dto;

import java.time.Instant;
import java.util.UUID;
import vn.it4409.g16.auth.user.UserAccount;

/** Thông tin tài khoản cho trang quản trị. Không có passwordHash. */
public record AccountResponse(UUID id, String email, String role, String status, Instant createdAt) {
    public static AccountResponse of(UserAccount u) {
        return new AccountResponse(u.getId(), u.getEmail(), u.getRole(), u.getStatus().name(), u.getCreatedAt());
    }
}
