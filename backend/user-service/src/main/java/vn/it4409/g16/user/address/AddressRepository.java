package vn.it4409.g16.user.address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Lưu ý: KHÔNG có phương thức nào chỉ tìm theo id. Mọi truy vấn đều bắt buộc kèm userId, nên không
 * thể vô tình viết code đọc/sửa địa chỉ của người khác (chống IDOR ngay từ thiết kế repository).
 * (findById kế thừa từ JpaRepository vẫn tồn tại nhưng không được dùng ở tầng service.)
 */
public interface AddressRepository extends JpaRepository<Address, UUID> {

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    @Query("select a from Address a where a.userId = :userId order by a.isDefault desc, a.createdAt asc")
    List<Address> findAllOfUser(@Param("userId") UUID userId);

    long countByUserId(UUID userId);

    Optional<Address> findFirstByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Bỏ cờ mặc định của mọi địa chỉ KHÁC của người dùng (gọi trước khi đặt mặc định mới). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.isDefault = false where a.userId = :userId and a.id <> :keepId and a.isDefault = true")
    int clearDefaultExcept(@Param("userId") UUID userId, @Param("keepId") UUID keepId);
}
