package vn.it4409.g16.user.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import vn.it4409.g16.user.profile.Profile;

/** Người dùng xem hồ sơ CỦA CHÍNH MÌNH -> trả số điện thoại đầy đủ (đã giải mã). */
public record ProfileResponse(String fullName, String phone, LocalDate dob, Instant updatedAt) {
    public static ProfileResponse of(Profile p) {
        return new ProfileResponse(p.getFullName(), p.getPhone(), p.getDob(), p.getUpdatedAt());
    }
}
