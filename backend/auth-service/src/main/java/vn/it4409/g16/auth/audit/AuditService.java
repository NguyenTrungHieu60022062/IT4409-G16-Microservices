package vn.it4409.g16.auth.audit;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.auth.security.ClientInfo;

/**
 * Nhật ký bảo mật: ai, làm gì, lúc nào, từ đâu, thành công hay không.
 * Quy tắc vàng: KHÔNG BAO GIỜ ghi mật khẩu, token, hay hash vào log.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final AuditRepository repo;
    private final Clock clock;

    public AuditService(AuditRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /**
     * REQUIRES_NEW: ghi trong transaction riêng. Nếu không, khi đăng nhập thất bại và transaction
     * chính bị rollback thì dòng "LOGIN_FAILURE" cũng biến mất — đúng thứ ta cần giữ lại nhất.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String event, UUID userId, String maskedSubject, ClientInfo client, boolean success) {
        String ip = client == null ? null : client.ip();
        String ua = client == null ? null : client.userAgent();
        repo.save(new AuditEvent(userId, event, maskedSubject, ip, ua, success, clock.instant()));
        log.info("event={} success={} user={} subject={} ip={}", event, success, userId, maskedSubject, ip);
    }
}
