package vn.it4409.g16.auth.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import vn.it4409.g16.auth.web.ApiException;

/**
 * Chính sách mật khẩu theo NIST SP 800-63B:
 * <ul>
 *   <li>Dài 8–128 ký tự. Độ dài quan trọng hơn độ "phức tạp".</li>
 *   <li>KHÔNG ép "phải có chữ hoa + số + ký tự đặc biệt" — quy tắc đó sinh ra "Password1!" dễ đoán.</li>
 *   <li>Chặn mật khẩu nằm trong danh sách hay bị lộ / phổ biến, và mật khẩu chứa email.</li>
 *   <li>Cho phép mọi ký tự Unicode (tiếng Việt có dấu, dấu cách...).</li>
 * </ul>
 */
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private final Set<String> common;

    public PasswordPolicy() throws IOException {
        try (var in = new BufferedReader(new InputStreamReader(
                new ClassPathResource("common-passwords.txt").getInputStream(), StandardCharsets.UTF_8))) {
            common = in.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * Chuẩn hoá Unicode NFKC: chữ "ệ" có thể được gõ thành 1 ký tự hoặc "e" + 2 dấu kết hợp tuỳ bàn
     * phím/hệ điều hành. Không chuẩn hoá thì cùng một mật khẩu gõ trên 2 máy lại ra 2 hash khác nhau.
     */
    public static String normalize(String password) {
        return password == null ? "" : Normalizer.normalize(password, Normalizer.Form.NFKC);
    }

    /** @param password đã normalize; @param email đã chuẩn hoá */
    public void validate(String password, String email) {
        List<String> errors = new ArrayList<>();
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) errors.add("Mật khẩu phải có ít nhất " + MIN_LENGTH + " ký tự.");
        if (length > MAX_LENGTH) errors.add("Mật khẩu không được dài quá " + MAX_LENGTH + " ký tự.");

        String lower = password.toLowerCase(Locale.ROOT);
        if (common.contains(lower)) {
            errors.add("Mật khẩu này quá phổ biến và dễ bị đoán. Hãy chọn mật khẩu khác.");
        }
        if (password.codePoints().distinct().count() <= 2 && length > 0) {
            errors.add("Mật khẩu không được chỉ lặp lại một vài ký tự.");
        }
        if (email != null) {
            String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            if (lower.equals(email) || (local.length() >= 4 && lower.contains(local))) {
                errors.add("Mật khẩu không được chứa email của bạn.");
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest("WEAK_PASSWORD", "Mật khẩu chưa đạt yêu cầu.", errors);
        }
    }
}
