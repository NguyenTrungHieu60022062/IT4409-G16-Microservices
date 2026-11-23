package vn.it4409.g16.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Chỉ một trường. Role có tồn tại hay không do AdminService kiểm trong DB. */
public record ChangeRoleRequest(
        @NotBlank(message = "Chưa chọn role.")
        @Pattern(regexp = "[A-Z_]{1,20}", message = "Mã role không hợp lệ.")
        String role) {}
