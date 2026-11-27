package vn.it4409.g16.user.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import vn.it4409.g16.user.address.Address;

/** Không trả userId (thừa, và là định danh nội bộ). */
public record AddressResponse(UUID id, String receiver, String phone, String line1, String ward,
                              String district, String province, @JsonProperty("isDefault") boolean isDefault) {
    public static AddressResponse of(Address a) {
        return new AddressResponse(a.getId(), a.getReceiver(), a.getPhone(), a.getLine1(), a.getWard(),
                a.getDistrict(), a.getProvince(), a.isDefault());
    }
}
