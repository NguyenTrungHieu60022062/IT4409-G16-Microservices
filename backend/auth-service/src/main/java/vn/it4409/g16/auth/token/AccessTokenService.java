package vn.it4409.g16.auth.token;

import com.nimbusds.jose.jwk.RSAKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import vn.it4409.g16.auth.config.AuthProperties;
import vn.it4409.g16.auth.rbac.Role;
import vn.it4409.g16.auth.rbac.RoleRepository;
import vn.it4409.g16.auth.user.UserAccount;

/**
 * Access token = JWT RS256, sống ngắn (15 phút). Các service khác tự kiểm chữ ký bằng public key
 * mà không phải gọi Auth Service -> không có điểm nghẽn. Đổi lại, token đã phát thì không thu hồi
 * ngay được — đó là lý do thời hạn phải ngắn.
 */
@Service
public class AccessTokenService {

    public record AccessToken(String value, long expiresInSeconds) {}

    private final JwtEncoder encoder;
    private final AuthProperties props;
    private final Clock clock;
    private final String keyId;

    private final RoleRepository roles;

    public AccessTokenService(JwtEncoder encoder, AuthProperties props, Clock clock, RSAKey rsaKey,
                              RoleRepository roles) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
        this.keyId = rsaKey.getKeyID();
        this.roles = roles;
    }

    public AccessToken issue(UserAccount user) {
        Instant now = clock.instant();
        // Quyền đọc từ DB ở MỖI lần cấp token (login/refresh): đổi quyền của role -> tối đa 15 phút
        // sau mọi token mới đều phản ánh quyền mới. Role không tồn tại -> lỗi, không cấp token "rỗng".
        Role role = roles.findById(user.getRole())
                .orElseThrow(() -> new IllegalStateException("Role không tồn tại: " + user.getRole()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .subject(user.getId().toString())  // định danh duy nhất mà các service dùng làm userId
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of(role.getCode()))
                // Service khác phân quyền theo "perms" (vd hasAuthority('product:write')), không theo tên role.
                .claim("perms", role.sortedPermissions())
                // KHÔNG đưa email/sđt vào token: payload JWT chỉ là base64, ai cầm token cũng đọc được.
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, props.accessTokenTtl().toSeconds());
    }
}
