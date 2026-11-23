package vn.it4409.g16.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
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
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.it4409.g16.user.config.SecurityConfig;
import vn.it4409.g16.user.config.UserProperties;

/** Các bài "tự tấn công" User Service. Chạy: ./mvnw test */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSecurityTest {

    static final String ORIGIN = "http://localhost:5173";
    static final RSAKey AUTH_KEY = generate();

    static RSAKey generate() {
        try {
            return new RSAKeyGenerator(2048).keyID("test").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Giả lập Auth Service: decoder dùng public key cục bộ + đúng bộ validator của code thật. */
    @TestConfiguration
    static class TestJwtConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder(UserProperties props) throws Exception {
            NimbusJwtDecoder d = NimbusJwtDecoder.withPublicKey(AUTH_KEY.toRSAPublicKey())
                    .signatureAlgorithm(SignatureAlgorithm.RS256).build();
            d.setJwtValidator(SecurityConfig.validator(props));
            return d;
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    // ------------------------------------------------------------------ helpers

    static String token(UUID userId, String role, RSAKey key, String audience, Instant exp) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("g16-auth-service").audience(audience).subject(userId.toString())
                .issueTime(new Date()).expirationTime(Date.from(exp))
                .claim("roles", List.of(role)).claim("perms", permsOf(role)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    /** Giống dữ liệu khởi tạo trong migration V2__rbac.sql của Auth Service. */
    static List<String> permsOf(String role) {
        return switch (role) {
            case "ADMIN" -> List.of("account:lock", "account:read", "order:create", "order:read:any",
                    "order:update-status", "product:write", "profile:read:any", "review:write", "role:assign",
                    "role:read");
            default -> List.of("order:create", "review:write");
        };
    }

    static String tokenWithPerms(UUID userId, String role, List<String> perms) throws Exception {
        Instant exp = Instant.now().plusSeconds(600);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("g16-auth-service").audience("g16-api").subject(userId.toString())
                .issueTime(new Date()).expirationTime(Date.from(exp))
                .claim("roles", List.of(role)).claim("perms", perms).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(AUTH_KEY.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(AUTH_KEY));
        return jwt.serialize();
    }

    static String tokenFor(UUID userId) throws Exception {
        return token(userId, "CUSTOMER", AUTH_KEY, "g16-api", Instant.now().plusSeconds(600));
    }

    MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder b, String token, Object body) throws Exception {
        return b.header("Authorization", "Bearer " + token).header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    static Map<String, Object> address(String receiver, boolean isDefault) {
        return Map.of("receiver", receiver, "phone", "0912345678", "line1", "Số 1 Đại Cồ Việt",
                "ward", "Bách Khoa", "district", "Hai Bà Trưng", "province", "Hà Nội", "isDefault", isDefault);
    }

    String createAddress(String token, Map<String, Object> body) throws Exception {
        String res = mvc.perform(withJson(post("/api/users/me/addresses"), token, body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asText();
    }

    JsonNode listAddresses(String token) throws Exception {
        return json.readTree(mvc.perform(get("/api/users/me/addresses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    // ================================================================== xác thực

    @Test
    @DisplayName("Không có token -> 401")
    void noTokenRejected() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me/addresses")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token ký bằng khoá không phải của Auth Service -> 401")
    void foreignKeyRejected() throws Exception {
        RSAKey attacker = new RSAKeyGenerator(2048).keyID("test").generate();
        String forged = token(UUID.randomUUID(), "ADMIN", attacker, "g16-api", Instant.now().plusSeconds(600));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token hết hạn hoặc sai audience -> 401")
    void expiredOrWrongAudienceRejected() throws Exception {
        UUID u = UUID.randomUUID();
        String expired = token(u, "CUSTOMER", AUTH_KEY, "g16-api", Instant.now().minusSeconds(3600));
        String otherApp = token(u, "CUSTOMER", AUTH_KEY, "other-app", Instant.now().plusSeconds(600));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + otherApp)).andExpect(status().isUnauthorized());
    }

    // ================================================================== hồ sơ

    @Test
    @DisplayName("Lần đầu GET /me tạo hồ sơ rỗng; PUT lưu và chuẩn hoá số điện thoại")
    void profileLifecycle() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").doesNotExist());

        mvc.perform(withJson(put("/api/users/me"), t,
                        Map.of("fullName", "  Nguyễn   Văn An ", "phone", "+84 912.345.678", "dob", "2002-05-20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.phone").value("0912345678"));
    }

    @Test
    @DisplayName("Dữ liệu không hợp lệ -> 400: sđt sai, tên có ký tự lạ, chưa đủ 13 tuổi")
    void profileValidation() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("phone", "12345")))
                .andExpect(status().isBadRequest());
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("fullName", "<script>alert(1)</script>")))
                .andExpect(status().isBadRequest());
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("dob", java.time.LocalDate.now().minusYears(5).toString())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DOB"));
    }

    @Test
    @DisplayName("Mass assignment: gửi kèm userId của người khác -> 400")
    void cannotOverrideUserId() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("fullName", "An", "userId", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Số điện thoại được MÃ HOÁ trong DB (AES-GCM), API vẫn trả bản rõ cho chủ sở hữu")
    void phoneEncryptedAtRest() throws Exception {
        UUID u = UUID.randomUUID();
        String t = tokenFor(u);
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("phone", "0987654321"))).andExpect(status().isOk());

        String stored = jdbc.queryForObject("select phone_enc from profiles where user_id = ?", String.class, u);
        assertThat(stored).startsWith("v1:").doesNotContain("0987654321");

        // Cùng 1 số, mã hoá 2 lần ra 2 chuỗi khác nhau (IV ngẫu nhiên) -> không so khớp được giữa các dòng.
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("phone", "0987654321"))).andExpect(status().isOk());
        String again = jdbc.queryForObject("select phone_enc from profiles where user_id = ?", String.class, u);
        assertThat(again).isNotEqualTo(stored);
    }

    // ================================================================== sổ địa chỉ

    @Test
    @DisplayName("IDOR: B không đọc/sửa/xoá/đặt mặc định được địa chỉ của A (404)")
    void cannotTouchOtherUsersAddress() throws Exception {
        String a = tokenFor(UUID.randomUUID());
        String b = tokenFor(UUID.randomUUID());
        String aAddr = createAddress(a, address("Chủ Nhà A", true));

        mvc.perform(withJson(put("/api/users/me/addresses/" + aAddr), b, address("Kẻ Gian", false)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/users/me/addresses/" + aAddr).header("Authorization", "Bearer " + b).header("Origin", ORIGIN))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/users/me/addresses/" + aAddr + "/default").header("Authorization", "Bearer " + b).header("Origin", ORIGIN))
                .andExpect(status().isNotFound());
        assertThat(listAddresses(b)).isEmpty();

        JsonNode list = listAddresses(a);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("receiver").asText()).isEqualTo("Chủ Nhà A");
    }

    @Test
    @DisplayName("Địa chỉ mặc định: luôn đúng 1 cái; xoá cái mặc định -> cái khác lên thay")
    void defaultAddressRules() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        String first = createAddress(t, address("Một", false));     // đầu tiên -> tự thành mặc định
        String second = createAddress(t, address("Hai", true));     // đặt mặc định -> "Một" bị bỏ cờ
        createAddress(t, address("Ba", false));

        JsonNode list = listAddresses(t);
        assertThat(list).filteredOn(n -> n.get("isDefault").asBoolean()).hasSize(1);
        assertThat(list.get(0).get("id").asText()).isEqualTo(second);

        mvc.perform(delete("/api/users/me/addresses/" + second).header("Authorization", "Bearer " + t).header("Origin", ORIGIN))
                .andExpect(status().isNoContent());
        list = listAddresses(t);
        assertThat(list).hasSize(2).filteredOn(n -> n.get("isDefault").asBoolean()).hasSize(1);
        assertThat(list.get(0).get("id").asText()).isEqualTo(first);
    }

    @Test
    @DisplayName("Giới hạn 10 địa chỉ -> địa chỉ thứ 11 bị từ chối (409)")
    void addressLimit() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        for (int i = 0; i < 10; i++) createAddress(t, address("Người nhận " + (char) ('A' + i), false));
        mvc.perform(withJson(post("/api/users/me/addresses"), t, address("Thứ Mười Một", false)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ADDRESS_LIMIT"));
    }

    @Test
    @DisplayName("Payload XSS / ký tự lạ trong địa chỉ -> 400 (allowlist)")
    void addressAllowlist() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        Map<String, Object> evil = new java.util.HashMap<>(address("An", false));
        evil.put("line1", "<img src=x onerror=alert(1)>");
        mvc.perform(withJson(post("/api/users/me/addresses"), t, evil)).andExpect(status().isBadRequest());
        evil.put("line1", "Số 1\u0000INJECT");   // ký tự điều khiển -> bị allowlist chặn
        mvc.perform(withJson(post("/api/users/me/addresses"), t, evil)).andExpect(status().isBadRequest());
        // Xuống dòng (CR/LF) là khoảng trắng -> được chuẩn hoá thành 1 dấu cách, không lọt vào DB
        evil.put("line1", "Số 1\r\nINJECT");
        mvc.perform(withJson(post("/api/users/me/addresses"), t, evil))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.line1").value("Số 1 INJECT"));
    }

    // ================================================================== phân quyền

    @Test
    @DisplayName("CUSTOMER gọi API admin -> 403; ADMIN -> 200 và chỉ thấy sđt đã che")
    void adminEndpoint() throws Exception {
        UUID u = UUID.randomUUID();
        String t = tokenFor(u);
        mvc.perform(withJson(put("/api/users/me"), t, Map.of("fullName", "Khách Hàng", "phone", "0911222333")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + t)).andExpect(status().isForbidden());

        String admin = token(UUID.randomUUID(), "ADMIN", AUTH_KEY, "g16-api", Instant.now().plusSeconds(600));
        String body = mvc.perform(get("/api/admin/users?size=100000").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))   // size bị chặn ở 50
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("091****333").doesNotContain("0911222333");
    }

    @Test
    @DisplayName("RBAC: quyết định theo permission, không theo tên role; đường admin lạ bị cấm")
    void permissionNotRoleNameDecides() throws Exception {
        // Role không phải ADMIN nhưng được cấp profile:read:any (vd role thêm sau này) -> vào được
        String granted = tokenWithPerms(UUID.randomUUID(), "CUSTOMER", List.of("profile:read:any"));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + granted)).andExpect(status().isOk());

        String roleOnly = tokenWithPerms(UUID.randomUUID(), "ADMIN", List.of());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + roleOnly))
                .andExpect(status().isForbidden());
        // Đường admin không khai báo -> cấm hẳn, kể cả với ADMIN
        String admin = token(UUID.randomUUID(), "ADMIN", AUTH_KEY, "g16-api", Instant.now().plusSeconds(600));
        mvc.perform(get("/api/admin/whatever").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST từ Origin lạ -> 403")
    void foreignOriginRejected() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        mvc.perform(post("/api/users/me/addresses").header("Authorization", "Bearer " + t)
                        .header("Origin", "https://evil.example").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(address("An", false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lỗi không lộ stack trace; header bảo mật có mặt")
    void noLeaks() throws Exception {
        String t = tokenFor(UUID.randomUUID());
        ResultActions r = mvc.perform(put("/api/users/me").header("Authorization", "Bearer " + t)
                .header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON).content("{broken"));
        r.andExpect(status().isBadRequest());
        assertThat(r.andReturn().getResponse().getContentAsString()).doesNotContain("Exception", "at org.");
        assertThat(r.andReturn().getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }
}
