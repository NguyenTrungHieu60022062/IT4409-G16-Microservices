package vn.it4409.g16.auth.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotNull(message = "Vui lòng nhập mật khẩu hiện tại.")
        @Size(max = 512, message = "Mật khẩu quá dài.")
        String currentPassword,
        @NotNull(message = "Vui lòng nhập mật khẩu mới.")
        @Size(max = 512, message = "Mật khẩu quá dài.")
        String newPassword) {}
