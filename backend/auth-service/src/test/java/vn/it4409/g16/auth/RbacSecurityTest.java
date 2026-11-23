package vn.it4409.g16.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.it4409.g16.auth.audit.AuditRepository;
import vn.it4409.g16.auth.user.UserAccount;
import vn.it4409.g16.auth.user.UserRepository;

/** Bài tự tấn công cho phân quyền RBAC: leo quyền, tự nâng quyền, dùng token cũ sau khi bị hạ quyền... */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacSecurityTest {

    static final String ORIGIN = "http://localhost:5173";
    static final String PW = "ca-phe-sang-thu-hai-2026";

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;
    @Autowired UserRepository users;
    @Autowired RSAKey rsaKey;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuditRepository audit;

    // ------------------------------------------------------------------ helpers

    record Account(UUID id, String email) {}

    Account newAccount() throws Exception {
        String email = "rbac-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mvc.perform(post("/api/auth/register").header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PW))))
                .andExpect(status().isAccepted());
        return new Account(users.findByEmail(email).orElseThrow().getId(), email);
    }

    /** Chỉ test mới được nâng quyền thẳng trong DB (mô phỏng AdminBootstrap). */
    Account newAdmin() throws Exception {
        Account a = newAccount();
        UserAccount u = users.findById(a.id()).orElseThrow();
        u.assignRole("ADMIN", clock.instant());
        users.save(u);
        return a;
    }

    MvcResult login(Account a) throws Exception {
        return mvc.perform(post("/api/auth/login").header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", a.email(), "password", PW, "rememberMe", false))))
                .andReturn();
    }

    String token(Account a) throws Exception {
        MvcResult r = login(a);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    JsonNode payload(String token) throws Exception {
        return json.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    }

    List<String> perms(String token) throws Exception {
        List<String> out = new ArrayList<>();
        payload(token).get("perms").forEach(n -> out.add(n.asText()));
        return out;
    }

    MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token).header("Origin", ORIGIN);
    }

    MockHttpServletRequestBuilder putJson(String url, String token, Object body) throws Exception {
        return authed(put(url), token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    // ================================================================== tests

    @Test
    @DisplayName("Tài khoản mới: role CUSTOMER, token chỉ có quyền của khách; /me trả về danh sách quyền")
    void newAccountGetsCustomerPermissions() throws Exception {
        String t = token(newAccount());
        assertThat(payload(t).get("roles").toString()).isEqualTo("[\"CUSTOMER\"]");
        assertThat(perms(t)).containsExactly("order:create", "review:write");

        mvc.perform(authed(get("/api/auth/me"), t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.permissions.length()").value(2));
    }

    @Test
    @DisplayName("Leo quyền dọc: CUSTOMER gọi API quản trị -> 403")
    void customerCannotUseAdminApi() throws Exception {
        Account victim = newAccount();
        String t = token(newAccount());
        mvc.perform(authed(get("/api/admin/accounts"), t)).andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/admin/roles"), t)).andExpect(status().isForbidden());
        mvc.perform(putJson("/api/admin/accounts/" + victim.id() + "/role", t, Map.of("role", "ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(putJson("/api/admin/accounts/" + victim.id() + "/status", t, Map.of("status", "DISABLED")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN xem được danh sách role và tài khoản (trang tối đa 50, không lộ password hash)")
    void adminCanReadRolesAndAccounts() throws Exception {
        String t = token(newAdmin());
        String roles = mvc.perform(authed(get("/api/admin/roles"), t))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(roles).contains("\"CUSTOMER\"", "\"ADMIN\"", "role:assign").doesNotContain("STAFF");

        String page = mvc.perform(authed(get("/api/admin/accounts?size=100000"), t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andReturn().getResponse().getContentAsString();
        assertThat(page).doesNotContain("argon2", "password");
    }

    @Test
    @DisplayName("Nâng CUSTOMER -> ADMIN: phiên cũ bị thu hồi, token cũ bị từ chối, token mới có quyền ADMIN")
    void changeRoleTakesEffectImmediately() throws Exception {
        String admin = token(newAdmin());
        Account user = newAccount();
        MvcResult oldLogin = login(user);
        String oldToken = json.readTree(oldLogin.getResponse().getContentAsString()).get("accessToken").asText();
        Cookie oldCookie = oldLogin.getResponse().getCookie("refresh_token");

        clock.advance(Duration.ofSeconds(2));
        mvc.perform(putJson("/api/admin/accounts/" + user.id() + "/role", admin, Map.of("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(authed(get("/api/auth/me"), oldToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REVOKED"));
        assertThat(mvc.perform(post("/api/auth/refresh").header("Origin", ORIGIN).cookie(oldCookie))
                .andReturn().getResponse().getStatus()).isEqualTo(401);

        String promoted = token(user);
        assertThat(payload(promoted).get("roles").toString()).isEqualTo("[\"ADMIN\"]");
        assertThat(perms(promoted)).contains("role:assign", "account:read", "profile:read:any");
        mvc.perform(authed(get("/api/admin/accounts"), promoted)).andExpect(status().isOk());

        assertThat(audit.findAll()).anyMatch(e -> e.getEvent().equals("ROLE_CHANGED")
                && user.id().equals(e.getUserId()) && e.getSubject().contains("CUSTOMER->ADMIN"));
    }

    @Test
    @DisplayName("Admin bị hạ quyền: token ADMIN còn hạn của họ bị từ chối ngay ở API quản trị")
    void demotedAdminLosesAccessImmediately() throws Exception {
        String a1 = token(newAdmin());
        Account a2 = newAdmin();
        String a2Token = token(a2);
        Account someone = newAccount();

        clock.advance(Duration.ofSeconds(2));
        mvc.perform(putJson("/api/admin/accounts/" + a2.id() + "/role", a1, Map.of("role", "CUSTOMER")))
                .andExpect(status().isOk());

        // Chữ ký và claim perms trong token vẫn "đúng", nhưng DB nói quyền đã bị đổi sau khi token được cấp.
        mvc.perform(putJson("/api/admin/accounts/" + someone.id() + "/role", a2Token, Map.of("role", "ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_REVOKED"));
        assertThat(users.findById(someone.id()).orElseThrow().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Không tự đổi role / tự khoá chính mình")
    void cannotChangeSelf() throws Exception {
        Account admin = newAdmin();
        String t = token(admin);
        mvc.perform(putJson("/api/admin/accounts/" + admin.id() + "/role", t, Map.of("role", "CUSTOMER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_CHANGE_SELF"));
        mvc.perform(putJson("/api/admin/accounts/" + admin.id() + "/status", t, Map.of("status", "DISABLED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_CHANGE_SELF"));
    }

    @Test
    @DisplayName("Role không tồn tại / sai định dạng / gửi kèm trường lạ -> 400; id không có -> 404")
    void invalidRoleRequests() throws Exception {
        String t = token(newAdmin());
        Account user = newAccount();
        String url = "/api/admin/accounts/" + user.id() + "/role";
        mvc.perform(putJson(url, t, Map.of("role", "STAFF")))   // role đã bỏ -> không tồn tại
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UNKNOWN_ROLE"));
        mvc.perform(putJson(url, t, Map.of("role", "SUPERUSER")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UNKNOWN_ROLE"));
        mvc.perform(putJson(url, t, Map.of("role", "admin' OR 1=1--")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(putJson(url, t, Map.of("role", "ADMIN", "status", "ACTIVE")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mvc.perform(putJson("/api/admin/accounts/" + UUID.randomUUID() + "/role", t, Map.of("role", "ADMIN")))
                .andExpect(status().isNotFound());
        assertThat(users.findById(user.id()).orElseThrow().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Khoá tài khoản: không đăng nhập được, phiên bị thu hồi; mở khoá thì vào lại được")
    void disableAndEnableAccount() throws Exception {
        String admin = token(newAdmin());
        Account user = newAccount();
        Cookie cookie = login(user).getResponse().getCookie("refresh_token");

        clock.advance(Duration.ofSeconds(2));
        mvc.perform(putJson("/api/admin/accounts/" + user.id() + "/status", admin, Map.of("status", "DISABLED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISABLED"));
        assertThat(login(user).getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(post("/api/auth/refresh").header("Origin", ORIGIN).cookie(cookie))
                .andReturn().getResponse().getStatus()).isEqualTo(401);

        mvc.perform(putJson("/api/admin/accounts/" + user.id() + "/status", admin, Map.of("status", "ACTIVE")))
                .andExpect(status().isOk());
        assertThat(login(user).getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * Tạo (một lần) role giả lập "OPERATOR": không phải ADMIN nhưng có role:assign, account:lock.
     * Mô phỏng một role tương lai do nhóm thêm bằng migration.
     */
    Account newOperator() throws Exception {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE code = 'OPERATOR'", Integer.class) == 0) {
            jdbc.update("INSERT INTO roles (code, name, description) VALUES ('OPERATOR', 'Vận hành', 'test')");
        }
        jdbc.update("DELETE FROM role_permissions WHERE role_code = 'OPERATOR'");
        jdbc.update("INSERT INTO role_permissions (role_code, permission_code) VALUES "
                + "('OPERATOR', 'role:assign'), ('OPERATOR', 'account:lock')");
        Account a = newAccount();
        UserAccount u = users.findById(a.id()).orElseThrow();
        u.assignRole("OPERATOR", clock.instant());
        users.save(u);
        return a;
    }

    @Test
    @DisplayName("Không bao giờ để hệ thống còn 0 ADMIN đang hoạt động -> 409 LAST_ADMIN")
    void cannotRemoveLastAdmin() throws Exception {
        Account onlyAdmin = newAdmin();
        // Các test khác dùng chung DB: hạ hết admin khác để onlyAdmin là admin cuối cùng.
        jdbc.update("UPDATE users SET role = 'CUSTOMER' WHERE role = 'ADMIN' AND id <> ?", onlyAdmin.id());
        String t = token(newOperator());

        mvc.perform(putJson("/api/admin/accounts/" + onlyAdmin.id() + "/role", t, Map.of("role", "CUSTOMER")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ADMIN"));
        mvc.perform(putJson("/api/admin/accounts/" + onlyAdmin.id() + "/status", t, Map.of("status", "DISABLED")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ADMIN"));
        assertThat(users.findById(onlyAdmin.id()).orElseThrow().getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Quyền bị gỡ khỏi role trong DB: token cũ còn claim perms vẫn bị 403 ở API quản trị")
    void adminApiChecksPermissionInDatabase() throws Exception {
        String t = token(newOperator());
        Account target = newAccount();
        assertThat(perms(t)).contains("role:assign");

        jdbc.update("DELETE FROM role_permissions WHERE role_code = 'OPERATOR' AND permission_code = 'role:assign'");
        mvc.perform(putJson("/api/admin/accounts/" + target.id() + "/role", t, Map.of("role", "ADMIN")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(users.findById(target.id()).orElseThrow().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Token giả mạo claim perms (ký đúng khoá) cho tài khoản CUSTOMER -> 403 ở API quản trị")
    void forgedPermsClaimRejectedByDatabaseCheck() throws Exception {
        Account customer = newAccount();
        Account victim = newAccount();
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject(customer.id().toString())
                .issuer("g16-auth-service").audience("g16-api")
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(600)))
                .claim("roles", List.of("CUSTOMER")).claim("perms", List.of("role:assign")).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(rsaKey));

        mvc.perform(putJson("/api/admin/accounts/" + victim.id() + "/role", jwt.serialize(), Map.of("role", "ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(users.findById(victim.id()).orElseThrow().getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("DB chặn role rác: không lưu được users.role không có trong bảng roles (khoá ngoại)")
    void databaseRejectsUnknownRole() throws Exception {
        Account user = newAccount();
        try {
            jdbc.update("UPDATE users SET role = 'HACKER' WHERE id = ?", user.id());
            throw new AssertionError("DB phải từ chối");
        } catch (org.springframework.dao.DataIntegrityViolationException expected) {
            assertThat(users.findById(user.id()).orElseThrow().getRole()).isEqualTo("CUSTOMER");
        }
    }
}
