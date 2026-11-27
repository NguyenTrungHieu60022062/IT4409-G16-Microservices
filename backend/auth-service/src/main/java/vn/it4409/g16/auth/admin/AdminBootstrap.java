package vn.it4409.g16.auth.admin;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.auth.audit.AuditService;
import vn.it4409.g16.auth.config.AuthProperties;
import vn.it4409.g16.auth.rbac.Role;
import vn.it4409.g16.auth.security.Emails;
import vn.it4409.g16.auth.token.RefreshTokenService;
import vn.it4409.g16.auth.user.UserRepository;

/**
 * Tạo quản trị viên ĐẦU TIÊN mà không phải ghi mật khẩu vào file migration (file đó nằm trên GitHub).
 *
 *   1. Đăng ký tài khoản bình thường qua /api/auth/register.
 *   2. Đặt AUTH_BOOTSTRAP_ADMIN_EMAIL=<email đó> rồi khởi động lại auth-service.
 *   3. Xoá biến môi trường đi.
 *
 * Chỉ có tác dụng khi hệ thống CHƯA có ADMIN nào: một khi đã có admin, biến này bị bỏ qua, nên
 * ai sửa được biến môi trường cũng không dùng nó để tự leo quyền về sau.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthProperties props;
    private final UserRepository users;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;
    private final Clock clock;

    public AdminBootstrap(AuthProperties props, UserRepository users, RefreshTokenService refreshTokens,
                          AuditService audit, Clock clock) {
        this.props = props;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String raw = props.bootstrapAdminEmail();
        if (raw == null || raw.isBlank()) return;
        if (users.countByRole(Role.ADMIN) > 0) {
            log.info("Đã có ADMIN -> bỏ qua AUTH_BOOTSTRAP_ADMIN_EMAIL (nên xoá biến này).");
            return;
        }
        String email = Emails.normalize(raw);
        users.findByEmail(email).ifPresentOrElse(u -> {
            u.assignRole(Role.ADMIN, clock.instant());
            users.save(u);
            refreshTokens.revokeAllOfUser(u.getId(), "ROLE_CHANGED");
            audit.record("ADMIN_BOOTSTRAP", u.getId(), Emails.mask(email), null, true);
            log.warn("Đã nâng {} lên ADMIN. Hãy XOÁ AUTH_BOOTSTRAP_ADMIN_EMAIL khỏi cấu hình.", Emails.mask(email));
        }, () -> log.warn("AUTH_BOOTSTRAP_ADMIN_EMAIL: chưa có tài khoản {} — đăng ký trước rồi khởi động lại.",
                Emails.mask(email)));
    }
}
