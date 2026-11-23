package vn.it4409.g16.user.address;

/** Dữ liệu địa chỉ đã được validate ở tầng web, truyền xuống tầng nghiệp vụ. */
public record AddressData(String receiver, String phone, String line1, String ward, String district, String province) {}
