package vn.it4409.g16.user.web.dto;

/**
 * Chuẩn hoá + mẫu validate dùng chung.
 *
 * <p>Dùng ALLOWLIST (chỉ cho phép ký tự hợp lệ) thay vì blocklist (cấm "<script>"...): blocklist
 * luôn có cách lách (viết hoa, encode, thẻ khác). Lưu ý allowlist chỉ là lớp phòng thủ thêm — chống
 * XSS chính vẫn là escape khi HIỂN THỊ (React tự làm), vì dữ liệu có thể đến từ nguồn khác.
 */
public final class Inputs {
    private Inputs() {}

    /** Chữ cái (kể cả tiếng Việt có dấu), khoảng trắng, dấu chấm, nháy đơn, gạch ngang. */
    public static final String NAME = "^[\\p{L}\\p{M}][\\p{L}\\p{M} .'\\-]*$";
    /** Di động Việt Nam sau chuẩn hoá: 0 + đầu số 3/5/7/8/9 + 8 chữ số. */
    public static final String PHONE_VN = "^0[35789][0-9]{8}$";
    /** Địa chỉ: chữ, số, khoảng trắng và , . / # ( ) ' - */
    public static final String ADDRESS = "^[\\p{L}\\p{M}\\p{N} ,./#()'\\-]+$";

    /** Trim, gộp nhiều khoảng trắng; chuỗi rỗng -> null. */
    public static String clean(String s) {
        if (s == null) return null;
        String t = s.strip().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /** "+84 912.345.678" -> "0912345678". */
    public static String phone(String s) {
        String t = clean(s);
        if (t == null) return null;
        t = t.replaceAll("[\\s.\\-()]", "");
        if (t.startsWith("+84")) t = "0" + t.substring(3);
        else if (t.startsWith("84") && t.length() == 11) t = "0" + t.substring(2);
        return t;
    }

    /** "0912345678" -> "091****678" — dùng cho log và màn hình admin. */
    public static String maskPhone(String p) {
        if (p == null) return null;
        if (p.length() < 7) return "****";
        return p.substring(0, 3) + "****" + p.substring(p.length() - 3);
    }
}
