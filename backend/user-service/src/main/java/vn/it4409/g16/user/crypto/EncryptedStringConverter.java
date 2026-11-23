package vn.it4409.g16.user.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Gắn @Convert(converter = EncryptedStringConverter.class) lên trường nào thì trường đó tự động được
 * mã hoá khi ghi và giải mã khi đọc. Code nghiệp vụ chỉ thấy bản rõ; database chỉ thấy bản mã.
 * (Spring Boot cho Hibernate lấy converter từ Spring context nên inject được PiiCipher.)
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final PiiCipher cipher;

    public EncryptedStringConverter(PiiCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return cipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return cipher.decrypt(dbData);
    }
}
