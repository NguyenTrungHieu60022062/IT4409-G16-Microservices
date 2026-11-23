package vn.it4409.g16.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập email.")
        @Size(max = 254, message = "Email quá dài.")
        String email,
        // Giới hạn độ dài trước khi băm: Argon2 với input cực dài vẫn tốn CPU (DoS).
        @NotNull(message = "Vui lòng nhập mật khẩu.")
        @Size(max = 512, message = "Mật khẩu quá dài.")
        String password,
        boolean rememberMe) {
    /** Bỏ khoảng trắng thừa khi copy-paste email, TRƯỚC khi Bean Validation chạy. */
    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
