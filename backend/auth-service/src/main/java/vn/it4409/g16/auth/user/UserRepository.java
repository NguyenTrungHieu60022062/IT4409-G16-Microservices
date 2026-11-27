package vn.it4409.g16.auth.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/** Spring Data sinh câu SQL có tham số (PreparedStatement) -> không có SQL injection qua email. */
public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(String role);

    Page<UserAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * SELECT ... FOR UPDATE trên mọi admin đang hoạt động. Hai admin cùng lúc hạ quyền lẫn nhau sẽ
     * phải chờ nhau thay vì cả hai cùng thấy "còn 2 admin" rồi cùng thành công -> hệ thống mất admin.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserAccount> findByRoleAndStatus(String role, UserAccount.Status status);
}
