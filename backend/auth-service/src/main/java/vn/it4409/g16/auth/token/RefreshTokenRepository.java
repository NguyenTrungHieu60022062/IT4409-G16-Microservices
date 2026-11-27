package vn.it4409.g16.auth.token;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Đánh dấu token đã được xoay — CHỈ khi nó chưa bị thu hồi. Trả về số dòng cập nhật.
     * Nếu 2 request dùng cùng token chạy song song, chỉ 1 request cập nhật được (trả 1), request
     * còn lại nhận 0. Đây là cách chống race condition "kiểm tra rồi mới ghi" (TOCTOU): điều kiện
     * và thao tác ghi nằm trong CÙNG một câu SQL nguyên tử.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokeReason = 'ROTATED', t.replacedBy = :newId "
            + "where t.id = :id and t.revokedAt is null")
    int markRotated(@Param("id") UUID id, @Param("newId") UUID newId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason "
            + "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("reason") String reason, @Param("now") Instant now);

    /** Có điều kiện userId: người dùng A không thể thu hồi phiên của người dùng B (chống IDOR). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason "
            + "where t.familyId = :familyId and t.userId = :userId and t.revokedAt is null")
    int revokeFamilyOfUser(@Param("familyId") UUID familyId, @Param("userId") UUID userId,
                           @Param("reason") String reason, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason "
            + "where t.userId = :userId and t.revokedAt is null")
    int revokeAllOfUser(@Param("userId") UUID userId, @Param("reason") String reason, @Param("now") Instant now);

    @Query("select t from RefreshToken t where t.userId = :userId and t.revokedAt is null and t.expiresAt > :now "
            + "order by t.createdAt desc")
    List<RefreshToken> findActiveByUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
