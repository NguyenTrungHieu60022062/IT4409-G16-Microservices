package vn.it4409.g16.auth.admin;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.it4409.g16.auth.AuthService;
import vn.it4409.g16.auth.audit.AuditService;
import vn.it4409.g16.auth.rbac.Role;
import vn.it4409.g16.auth.rbac.RoleRepository;
import vn.it4409.g16.auth.security.ClientInfo;
import vn.it4409.g16.auth.security.Emails;
import vn.it4409.g16.auth.token.RefreshTokenService;
import vn.it4409.g16.auth.user.UserAccount;
import vn.it4409.g16.auth.user.UserAccount.Status;
import vn.it4409.g16.auth.user.UserRepository;
import vn.it4409.g16.auth.web.ApiException;

/**
 * Quản trị tài khoản & phân quyền. Controller đã kiểm permission trong token (lớp 1, 2); service này
 * kiểm lại permission theo DB (lớp 3) và các QUY TẮC NGHIỆP VỤ mà permission không diễn tả được:
 *  1. Không tự đổi role / tự khoá chính mình (tránh tự leo quyền và tự khoá mất quyền quản trị).
 *  2. Không được để hệ thống còn 0 ADMIN đang hoạt động.
 *  3. Đổi quyền xong -> thu hồi mọi phiên của người bị đổi, để quyền mới có hiệu lực ngay.
 *  4. Mọi thay đổi đều ghi audit log: ai đổi, đổi cho ai, từ gì sang gì.
 */
@Service
public class AdminService {

    static final int MAX_PAGE_SIZE = 50;

    private final AuthService auth;
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;
    private final Clock clock;

    public AdminService(AuthService auth, UserRepository users, RoleRepository roles,
                        RefreshTokenService refreshTokens, AuditService audit, Clock clock) {
        this.auth = auth;
        this.users = users;
        this.roles = roles;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.clock = clock;
    }

    public List<Role> listRoles(Jwt actorJwt) {
        requireActor(actorJwt, "role:read");
        return roles.findAll().stream().sorted((a, b) -> a.getCode().compareTo(b.getCode())).toList();
    }

    public Page<UserAccount> listAccounts(Jwt actorJwt, int page, int size) {
        requireActor(actorJwt, "account:read");
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return users.findAllByOrderByCreatedAtDesc(PageRequest.of(p, s));
    }

    public UserAccount getAccount(Jwt actorJwt, UUID id) {
        requireActor(actorJwt, "account:read");
        return users.findById(id).orElseThrow(AdminService::notFound);
    }

    @Transactional
    public UserAccount changeRole(Jwt actorJwt, UUID targetId, String roleCode, ClientInfo client) {
        // Khoá các dòng admin TRƯỚC, rồi mới kiểm người thao tác: nếu cùng lúc có người vừa hạ quyền
        // chính người thao tác, ta phải chờ giao dịch đó xong và đọc trạng thái mới nhất.
        List<UserAccount> activeAdmins = lockActiveAdmins();
        UserAccount actor = requireActor(actorJwt, "role:assign");
        if (actor.getId().equals(targetId)) {
            throw ApiException.badRequest("CANNOT_CHANGE_SELF", "Không thể tự đổi role của chính mình.", List.of());
        }
        Role role = roles.findById(roleCode).orElseThrow(() ->
                ApiException.badRequest("UNKNOWN_ROLE", "Role không tồn tại.", List.of()));

        UserAccount target = users.findById(targetId).orElseThrow(AdminService::notFound);
        String oldRole = target.getRole();
        if (oldRole.equals(role.getCode())) return target;

        if (Role.ADMIN.equals(oldRole) && target.isActive()) ensureNotLastAdmin(activeAdmins);

        target.assignRole(role.getCode(), clock.instant());
        users.save(target);
        refreshTokens.revokeAllOfUser(target.getId(), "ROLE_CHANGED");
        audit.record("ROLE_CHANGED", target.getId(),
                "by=" + actor.getId() + " " + oldRole + "->" + role.getCode(), client, true);
        return target;
    }

    @Transactional
    public UserAccount changeStatus(Jwt actorJwt, UUID targetId, Status status, ClientInfo client) {
        List<UserAccount> activeAdmins = lockActiveAdmins();
        UserAccount actor = requireActor(actorJwt, "account:lock");
        if (actor.getId().equals(targetId)) {
            throw ApiException.badRequest("CANNOT_CHANGE_SELF", "Không thể tự khoá / mở khoá chính mình.", List.of());
        }
        UserAccount target = users.findById(targetId).orElseThrow(AdminService::notFound);
        if (target.getStatus() == status) return target;

        if (status == Status.DISABLED && Role.ADMIN.equals(target.getRole())) ensureNotLastAdmin(activeAdmins);

        target.changeStatus(status, clock.instant());
        users.save(target);
        if (status == Status.DISABLED) refreshTokens.revokeAllOfUser(target.getId(), "ACCOUNT_DISABLED");
        audit.record(status == Status.DISABLED ? "ACCOUNT_DISABLED" : "ACCOUNT_ENABLED", target.getId(),
                "by=" + actor.getId() + " " + Emails.mask(target.getEmail()), client, true);
        return target;
    }

    /**
     * Người thao tác phải: (1) có token còn hiệu lực theo DB — admin vừa bị hạ quyền hay bị khoá thì
     * token cũ bị từ chối ngay; (2) có permission theo DB, không chỉ theo claim "perms" trong token.
     * Lý do (2): khi một migration bỏ quyền khỏi role, token cũ vẫn mang quyền cũ tới 15 phút. Với API
     * quản trị (nhạy cảm nhất), Auth có sẵn DB nên kiểm lại cho chắc.
     */
    private UserAccount requireActor(Jwt actorJwt, String permission) {
        UserAccount actor = auth.me(actorJwt);
        boolean allowed = roles.findById(actor.getRole()).map(r -> r.hasPermission(permission)).orElse(false);
        if (!allowed) {
            throw ApiException.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.");
        }
        return actor;
    }

    private List<UserAccount> lockActiveAdmins() {
        return users.findByRoleAndStatus(Role.ADMIN, Status.ACTIVE);
    }

    private static void ensureNotLastAdmin(List<UserAccount> activeAdmins) {
        if (activeAdmins.size() <= 1) {
            throw ApiException.of(HttpStatus.CONFLICT, "LAST_ADMIN",
                    "Không thể thực hiện: hệ thống phải còn ít nhất một quản trị viên đang hoạt động.");
        }
    }

    /** 404 chung: không xác nhận cho người dò rằng id có tồn tại hay không. */
    private static ApiException notFound() {
        return ApiException.of(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản.");
    }
}
