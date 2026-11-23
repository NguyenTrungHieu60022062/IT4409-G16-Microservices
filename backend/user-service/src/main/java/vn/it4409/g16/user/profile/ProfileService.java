package vn.it4409.g16.user.profile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.user.web.ApiException;

@Service
public class ProfileService {

    static final LocalDate MIN_DOB = LocalDate.of(1900, 1, 1);
    static final int MIN_AGE = 13;

    private final ProfileRepository repo;
    private final Clock clock;

    public ProfileService(ProfileRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /**
     * Tạo hồ sơ rỗng ở lần truy cập đầu tiên (lazy create) — không cần Auth Service gọi sang khi đăng
     * ký, nên 2 service không phụ thuộc nhau. Hai request đầu tiên chạy song song có thể cùng INSERT:
     * PRIMARY KEY chặn bản thứ hai, ta bắt lỗi và đọc lại.
     */
    public Profile getOrCreate(UUID userId) {
        return repo.findById(userId).orElseGet(() -> {
            try {
                return repo.saveAndFlush(Profile.empty(userId, clock.instant()));
            } catch (DataIntegrityViolationException race) {
                return repo.findById(userId).orElseThrow();
            }
        });
    }

    @Transactional
    public Profile update(UUID userId, String fullName, String phone, LocalDate dob) {
        if (dob != null) {
            LocalDate today = LocalDate.now(clock);
            if (dob.isBefore(MIN_DOB) || dob.isAfter(today.minusYears(MIN_AGE))) {
                throw ApiException.badRequest("INVALID_DOB",
                        "Ngày sinh không hợp lệ (bạn cần từ " + MIN_AGE + " tuổi trở lên).", List.of());
            }
        }
        Profile p = repo.findById(userId).orElseGet(() -> Profile.empty(userId, clock.instant()));
        p.update(fullName, phone, dob, clock.instant());
        return repo.save(p);
    }

    @Transactional(readOnly = true)
    public Page<Profile> list(int page, int size) {
        // Giới hạn kích thước trang: size=1000000 không được kéo cả bảng ra (DoS / lộ dữ liệu hàng loạt).
        int safeSize = Math.min(Math.max(size, 1), 50);
        return repo.findAll(PageRequest.of(Math.max(page, 0), safeSize, Sort.by("createdAt").descending()));
    }
}
