package vn.it4409.g16.user.address;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.user.config.UserProperties;
import vn.it4409.g16.user.profile.ProfileRepository;
import vn.it4409.g16.user.web.ApiException;

@Service
public class AddressService {

    private final AddressRepository repo;
    private final ProfileRepository profiles;
    private final UserProperties props;
    private final Clock clock;

    public AddressService(AddressRepository repo, ProfileRepository profiles,
                          UserProperties props, Clock clock) {
        this.repo = repo;
        this.profiles = profiles;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Address> list(UUID userId) {
        return repo.findAllOfUser(userId);
    }

    /**
     * Controller phải gọi ProfileService.getOrCreate(userId) TRƯỚC (dòng cha phải tồn tại để khoá).
     * Lưu ý: không gọi method @Transactional khác trong cùng class — lời gọi nội bộ đi vòng qua proxy
     * của Spring nên annotation không có tác dụng.
     */
    @Transactional
    public Address create(UUID userId, AddressData data, boolean makeDefault) {
        // Khoá dòng hồ sơ -> các request thêm địa chỉ của cùng người dùng chạy tuần tự,
        // nên "đếm rồi thêm" không bị 2 request cùng lọt qua khi đang có 9 địa chỉ.
        profiles.lockById(userId).orElseThrow();
        long count = repo.countByUserId(userId);
        if (count >= props.maxAddresses()) {
            throw ApiException.of(HttpStatus.CONFLICT, "ADDRESS_LIMIT",
                    "Bạn chỉ lưu được tối đa " + props.maxAddresses() + " địa chỉ.");
        }
        boolean isDefault = makeDefault || count == 0; // địa chỉ đầu tiên luôn là mặc định
        Address a = Address.create(userId, data, isDefault, clock.instant());
        if (isDefault) repo.clearDefaultExcept(userId, a.getId());
        return repo.save(a);
    }

    @Transactional
    public Address update(UUID userId, UUID id, AddressData data, boolean makeDefault) {
        Address a = mine(userId, id);
        Instant now = clock.instant();
        if (makeDefault && !a.isDefault()) {
            repo.clearDefaultExcept(userId, id);
            a = mine(userId, id); // clearDefaultExcept xoá persistence context -> nạp lại
            a.setDefault(true, now);
        }
        a.apply(data, now);
        return repo.save(a);
    }

    @Transactional
    public Address setDefault(UUID userId, UUID id) {
        mine(userId, id);
        repo.clearDefaultExcept(userId, id);
        Address a = mine(userId, id);
        a.setDefault(true, clock.instant());
        return repo.save(a);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Address a = mine(userId, id);
        boolean wasDefault = a.isDefault();
        repo.delete(a);
        repo.flush();
        if (wasDefault) {
            // Xoá địa chỉ mặc định -> địa chỉ cũ nhất còn lại trở thành mặc định.
            repo.findFirstByUserIdOrderByCreatedAtAsc(userId).ifPresent(next -> {
                next.setDefault(true, clock.instant());
                repo.save(next);
            });
        }
    }

    /**
     * Tìm địa chỉ theo (id, userId). Không thuộc về người gọi -> 404 "không tìm thấy", KHÔNG phải 403
     * "không có quyền": 403 sẽ xác nhận cho kẻ dò rằng id đó tồn tại.
     */
    private Address mine(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.of(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ."));
    }
}
