package vn.it4409.g16.auth.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.it4409.g16.auth.security.ClientInfo;
import vn.it4409.g16.auth.user.UserAccount;
import vn.it4409.g16.auth.web.dto.AccountResponse;
import vn.it4409.g16.auth.web.dto.ChangeRoleRequest;
import vn.it4409.g16.auth.web.dto.ChangeStatusRequest;
import vn.it4409.g16.auth.web.dto.PageResponse;
import vn.it4409.g16.auth.web.dto.RoleResponse;

/**
 * API quản trị. Mỗi method có @PreAuthorize theo PERMISSION (lớp 2), trùng với luật theo URL trong
 * SecurityConfig (lớp 1). Quên một lớp thì lớp kia vẫn chặn.
 */
@RestController
@RequestMapping(path = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @PreAuthorize("hasAuthority('role:read')")
    @GetMapping("/roles")
    public List<RoleResponse> roles(@AuthenticationPrincipal Jwt jwt) {
        return admin.listRoles(jwt).stream().map(RoleResponse::of).toList();
    }

    @PreAuthorize("hasAuthority('account:read')")
    @GetMapping("/accounts")
    public PageResponse<AccountResponse> accounts(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        Page<UserAccount> p = admin.listAccounts(jwt, page, size);
        return new PageResponse<>(p.map(AccountResponse::of).getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements());
    }

    @PreAuthorize("hasAuthority('account:read')")
    @GetMapping("/accounts/{id}")
    public AccountResponse account(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return AccountResponse.of(admin.getAccount(jwt, id));
    }

    @PreAuthorize("hasAuthority('role:assign')")
    @PutMapping(path = "/accounts/{id}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody ChangeRoleRequest body, HttpServletRequest req) {
        return AccountResponse.of(admin.changeRole(jwt, id, body.role(), ClientInfo.from(req)));
    }

    @PreAuthorize("hasAuthority('account:lock')")
    @PutMapping(path = "/accounts/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                        @Valid @RequestBody ChangeStatusRequest body, HttpServletRequest req) {
        return AccountResponse.of(admin.changeStatus(jwt, id, body.status(), ClientInfo.from(req)));
    }
}
