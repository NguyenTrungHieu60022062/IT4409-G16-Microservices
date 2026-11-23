package vn.it4409.g16.user.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.it4409.g16.user.address.AddressService;
import vn.it4409.g16.user.profile.Profile;
import vn.it4409.g16.user.profile.ProfileService;
import vn.it4409.g16.user.security.CurrentUser;
import vn.it4409.g16.user.web.dto.AddressRequest;
import vn.it4409.g16.user.web.dto.AddressResponse;
import vn.it4409.g16.user.web.dto.AdminUserResponse;
import vn.it4409.g16.user.web.dto.PageResponse;
import vn.it4409.g16.user.web.dto.ProfileRequest;
import vn.it4409.g16.user.web.dto.ProfileResponse;

/**
 * Để ý: các đường dẫn đều là /me/... — KHÔNG có /users/{userId}/... cho người dùng thường.
 * Người dùng chỉ thao tác được trên dữ liệu của chính họ, xác định bằng JWT.
 */
@RestController
public class UserController {

    private final ProfileService profiles;
    private final AddressService addresses;

    public UserController(ProfileService profiles, AddressService addresses) {
        this.profiles = profiles;
        this.addresses = addresses;
    }

    // ---------------------------------------------------------------- hồ sơ

    @GetMapping("/api/users/me")
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.of(profiles.getOrCreate(CurrentUser.id(jwt)));
    }

    @PutMapping(path = "/api/users/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest body) {
        return ProfileResponse.of(profiles.update(CurrentUser.id(jwt), body.fullName(), body.phone(), body.dob()));
    }

    // ---------------------------------------------------------------- sổ địa chỉ

    @GetMapping("/api/users/me/addresses")
    public List<AddressResponse> listAddresses(@AuthenticationPrincipal Jwt jwt) {
        return addresses.list(CurrentUser.id(jwt)).stream().map(AddressResponse::of).toList();
    }

    @PostMapping(path = "/api/users/me/addresses", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> createAddress(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody AddressRequest body) {
        UUID userId = CurrentUser.id(jwt);
        profiles.getOrCreate(userId); // dòng cha phải có trước khi khoá & thêm địa chỉ
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AddressResponse.of(addresses.create(userId, body.toData(), body.isDefault())));
    }

    @PutMapping(path = "/api/users/me/addresses/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AddressResponse updateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                         @Valid @RequestBody AddressRequest body) {
        return AddressResponse.of(addresses.update(CurrentUser.id(jwt), id, body.toData(), body.isDefault()));
    }

    @PatchMapping("/api/users/me/addresses/{id}/default")
    public AddressResponse setDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return AddressResponse.of(addresses.setDefault(CurrentUser.id(jwt), id));
    }

    @DeleteMapping("/api/users/me/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        addresses.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- quản trị

    /** 2 lớp phân quyền: URL (SecurityConfig) + @PreAuthorize ở method. Mặc định chỉ ADMIN có quyền này. */
    @PreAuthorize("hasAuthority('profile:read:any')")
    @GetMapping("/api/admin/users")
    public PageResponse<AdminUserResponse> adminList(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Page<Profile> p = profiles.list(page, size);
        return new PageResponse<>(p.map(AdminUserResponse::of).getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
    }
}
