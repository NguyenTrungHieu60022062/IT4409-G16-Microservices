package vn.it4409.g16.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

/**
 * Khoá ký JWT RS256.
 *
 * <p>Vì sao RS256 (bất đối xứng) mà không phải HS256 (bí mật chung)? Với HS256, service nào kiểm
 * được token cũng KÝ được token — chỉ cần một service của nhóm bị lộ secret là kẻ tấn công tự cấp
 * token ADMIN. Với RS256, chỉ Auth Service giữ private key; các service khác chỉ có public key
 * (lấy qua /.well-known/jwks.json) nên chỉ kiểm được, không giả mạo được.
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RSAKey rsaKey(AuthProperties props, ResourceLoader resources) throws Exception {
        RSAPrivateKey privateKey;
        RSAPublicKey publicKey;
        String location = props.jwt().privateKeyLocation();
        if (StringUtils.hasText(location)) {
            try (InputStream in = resources.getResource(location).getInputStream()) {
                privateKey = RsaKeyConverters.pkcs8().convert(in);
            }
            // Suy public key từ private key (RSA CRT chứa sẵn modulus + public exponent).
            RSAPrivateCrtKey crt = (RSAPrivateCrtKey) privateKey;
            publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
            log.info("Đã nạp khoá ký JWT từ {}", location);
        } else {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(3072);
            KeyPair pair = gen.generateKeyPair();
            privateKey = (RSAPrivateKey) pair.getPrivate();
            publicKey = (RSAPublicKey) pair.getPublic();
            log.warn("Chưa cấu hình AUTH_JWT_PRIVATE_KEY_LOCATION -> dùng khoá RSA TẠM THỜI. "
                    + "Mọi access token sẽ mất hiệu lực khi khởi động lại. Không dùng cho môi trường thật.");
        }
        RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        // kid = thumbprint RFC 7638: cùng khoá -> cùng kid, giúp các service chọn đúng khoá khi xoay khoá.
        return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Decoder dùng cho chính Auth Service (các endpoint /me, /sessions...). Các service khác cấu hình
     * tương đương qua jwk-set-uri — xem backend/docs/security-contract.md.
     */
    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, AuthProperties props) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey())
                // Chỉ chấp nhận RS256: token "alg":"none" hay "alg":"HS256" (tấn công nhầm thuật toán) bị loại.
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(props.audience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()), // exp, nbf, iss
                audience));
        return decoder;
    }
}
