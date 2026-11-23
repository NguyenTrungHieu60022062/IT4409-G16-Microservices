package vn.it4409.g16.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO chỉ chứa đúng những trường được phép gửi lên. Không có "role", "status", "id"... nên client
 * không thể tự gán quyền (mass assignment). Gửi thêm trường lạ -> 400 (fail-on-unknown-properties).
 */
public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập email.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 254, message = "Email quá dài.")
        String email,
        @NotNull(message = "Vui lòng nhập mật khẩu.")
        @Size(max = 512, message = "Mật khẩu quá dài.")
        String password) {
    /** Bỏ khoảng trắng thừa khi copy-paste email, TRƯỚC khi Bean Validation chạy. */
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }
}
