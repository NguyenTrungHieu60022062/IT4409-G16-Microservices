package vn.it4409.g16.user.web.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Chỉ 3 trường được phép sửa. Không có userId/email/role/createdAt -> gửi kèm sẽ bị 400.
 * Mọi trường đều không bắt buộc (null = xoá giá trị).
 */
public record ProfileRequest(
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
        @Pattern(regexp = Inputs.NAME, message = "Họ tên chỉ gồm chữ cái, khoảng trắng và dấu . ' -")
        String fullName,
        @Pattern(regexp = Inputs.PHONE_VN, message = "Số điện thoại không hợp lệ (VD: 0912345678).")
        String phone,
        @Past(message = "Ngày sinh phải ở quá khứ.")
        LocalDate dob) {

    public ProfileRequest {
        fullName = Inputs.clean(fullName);
        phone = Inputs.phone(phone);
    }
}
