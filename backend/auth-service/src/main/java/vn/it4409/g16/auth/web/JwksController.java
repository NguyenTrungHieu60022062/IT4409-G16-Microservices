package vn.it4409.g16.auth.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Công bố PUBLIC key để các service khác kiểm chữ ký JWT.
 * toPublicJWK() là bắt buộc: trả nhầm cả RSAKey sẽ lộ tham số "d" (private exponent) -> ai cũng
 * ký được token. Có test kiểm tra điều này (AuthSecurityTest.jwksNeverLeaksPrivateKey).
 */
@RestController
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(RSAKey rsaKey) {
        this.jwks = new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(jwks);
    }
}
