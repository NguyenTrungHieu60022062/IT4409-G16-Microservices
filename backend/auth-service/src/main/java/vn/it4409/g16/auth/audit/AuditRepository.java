package vn.it4409.g16.auth.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByUserIdOrderByIdAsc(UUID userId);
}
