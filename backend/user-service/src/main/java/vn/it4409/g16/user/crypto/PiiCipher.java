package vn.it4409.g16.user.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import vn.it4409.g16.user.config.UserProperties;

/**
 * Mã hoá dữ liệu cá nhân (PII) trước khi ghi xuống DB — "encryption at rest" ở tầng ứng dụng.
 *
 * <p>AES-256-GCM: vừa mã hoá vừa xác thực (tag 128 bit). Sửa 1 bit trong ciphertext -> giải mã
 * thất bại thay vì ra dữ liệu rác. IV 12 byte ngẫu nhiên MỚI cho mỗi lần mã hoá: dùng lại IV với
 * cùng khoá trong GCM là lỗi nghiêm trọng (lộ được XOR của 2 bản rõ và giả mạo được tag).
 *
 * <p>Định dạng lưu: "v1:" + base64(iv || ciphertext || tag). Tiền tố phiên bản cho phép đổi
 * thuật toán / xoay khoá sau này mà vẫn đọc được dữ liệu cũ.
 */
@Component
public class PiiCipher {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public PiiCipher(UserProperties props) {
        if (props.piiKey() == null || props.piiKey().isBlank()) {
            // Fail fast: thà không khởi động còn hơn âm thầm lưu số điện thoại dạng rõ.
            throw new IllegalStateException("Thiếu USER_PII_KEY (base64 của 32 byte). Tạo bằng: openssl rand -base64 32");
        }
        byte[] raw = Base64.getDecoder().decode(props.piiKey().trim());
        if (raw.length != 32) {
            throw new IllegalStateException("USER_PII_KEY phải là 32 byte (AES-256), hiện là " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không mã hoá được dữ liệu", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) return null;
        if (!stored.startsWith(PREFIX)) throw new IllegalStateException("Định dạng dữ liệu mã hoá không hợp lệ");
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            return new String(c.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Sai khoá hoặc dữ liệu bị sửa -> tag không khớp. Không trả về dữ liệu rác.
            throw new IllegalStateException("Không giải mã được dữ liệu (sai khoá hoặc dữ liệu bị sửa đổi)", e);
        }
    }
}
