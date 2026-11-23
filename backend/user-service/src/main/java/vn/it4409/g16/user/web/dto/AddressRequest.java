package vn.it4409.g16.user.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.it4409.g16.user.address.AddressData;

public record AddressRequest(
        @NotBlank(message = "Vui lòng nhập người nhận.")
        @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự.")
        @Pattern(regexp = Inputs.NAME, message = "Tên người nhận chỉ gồm chữ cái, khoảng trắng và dấu . ' -")
        String receiver,
        @NotBlank(message = "Vui lòng nhập số điện thoại.")
        @Pattern(regexp = Inputs.PHONE_VN, message = "Số điện thoại không hợp lệ (VD: 0912345678).")
        String phone,
        @NotBlank(message = "Vui lòng nhập địa chỉ cụ thể.")
        @Size(max = 200, message = "Địa chỉ cụ thể tối đa 200 ký tự.")
        @Pattern(regexp = Inputs.ADDRESS, message = "Địa chỉ chứa ký tự không hợp lệ.")
        String line1,
        @NotBlank(message = "Vui lòng nhập phường/xã.")
        @Size(max = 100, message = "Phường/xã tối đa 100 ký tự.")
        @Pattern(regexp = Inputs.ADDRESS, message = "Phường/xã chứa ký tự không hợp lệ.")
        String ward,
        @NotBlank(message = "Vui lòng nhập quận/huyện.")
        @Size(max = 100, message = "Quận/huyện tối đa 100 ký tự.")
        @Pattern(regexp = Inputs.ADDRESS, message = "Quận/huyện chứa ký tự không hợp lệ.")
        String district,
        @NotBlank(message = "Vui lòng nhập tỉnh/thành phố.")
        @Size(max = 100, message = "Tỉnh/thành phố tối đa 100 ký tự.")
        @Pattern(regexp = Inputs.ADDRESS, message = "Tỉnh/thành phố chứa ký tự không hợp lệ.")
        String province,
        @JsonProperty("isDefault")
        boolean isDefault) {

    public AddressRequest {
        receiver = Inputs.clean(receiver);
        phone = Inputs.phone(phone);
        line1 = Inputs.clean(line1);
        ward = Inputs.clean(ward);
        district = Inputs.clean(district);
        province = Inputs.clean(province);
    }

    public AddressData toData() {
        return new AddressData(receiver, phone, line1, ward, district, province);
    }
}
