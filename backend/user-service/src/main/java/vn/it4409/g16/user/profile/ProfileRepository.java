package vn.it4409.g16.user.profile;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    /**
     * SELECT ... FOR UPDATE: khoá dòng hồ sơ đến hết transaction. Dùng khi thêm địa chỉ để các request
     * song song của CÙNG một người dùng xếp hàng, tránh vượt giới hạn 10 địa chỉ (race condition).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.userId = :id")
    Optional<Profile> lockById(@Param("id") UUID id);
}
