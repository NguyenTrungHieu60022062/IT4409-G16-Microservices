package vn.it4409.g16.auth.security;

import java.util.Locale;

public final class Emails {
    private Emails() {}

    /** "  Ban@Example.COM " -> "ban@example.com". Không chuẩn hoá thì 2 cách viết hoa tạo được 2 tài khoản. */
    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** "nguyenvana@gmail.com" -> "ng***@gmail.com" — dùng cho log/audit để không rải PII khắp nơi. */
    public static String mask(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String shown = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        String masked = shown + "***" + email.substring(at);
        return masked.length() > 254 ? masked.substring(0, 254) : masked;
    }
}
