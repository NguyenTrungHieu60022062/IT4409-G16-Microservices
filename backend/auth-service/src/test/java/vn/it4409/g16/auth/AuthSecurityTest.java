package vn.it4409.g16.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.it4409.g16.auth.audit.AuditRepository;
import vn.it4409.g16.auth.security.RateLimiter;
import vn.it4409.g16.auth.web.ApiException;

/**
 * Mỗi test là một "bài tự tấn công" trong kế hoạch: ta đóng vai kẻ tấn công và khẳng định hệ thống
 * chặn được. Chạy: ./mvnw test
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityTest {

    static final String ORIGIN = "http://localhost:5173";
    static final String GOOD_PASSWORD = "ca-phe-sang-thu-hai-2026";

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
    @Autowired RSAKey rsaKey;
    @Autowired AuditRepository auditLog;

    // ------------------------------------------------------------------ helpers

    static String newEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    MockHttpServletRequestBuilder jsonPost(String url, Object body) throws Exception {
        return post(url).header("Origin", ORIGIN).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
    }

    void register(String email, String password) throws Exception {
        mvc.perform(jsonPost("/api/auth/register", new Reg(email, password))).andExpect(status().isAccepted());
    }

    MvcResult login(String email, String password, boolean remember) throws Exception {
        return mvc.perform(jsonPost("/api/auth/login", new Login(email, password, remember))).andReturn();
    }

    record Reg(String email, String password) {}
    record Login(String email, String password, boolean rememberMe) {}
    record ChangePw(String currentPassword, String newPassword) {}

    String accessToken(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
    }

    Cookie refreshCookie(MvcResult r) {
        return r.getResponse().getCookie("refresh_token");
    }

    MvcResult refresh(Cookie c) throws Exception {
        return mvc.perform(post("/api/auth/refresh").header("Origin", ORIGIN).cookie(c)).andReturn();
    }

    JsonNode jwtPayload(String token) throws Exception {
        return json.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    }

    // ================================================================== tests

    @Nested
    @DisplayName("Đăng ký & mật khẩu")
    class Registration {

        @Test
        @DisplayName("Đăng ký rồi đăng nhập: nhận access token + cookie HttpOnly/Secure/SameSite=Strict")
        void registerThenLogin() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            MvcResult r = login(email, GOOD_PASSWORD, false);

            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            String setCookie = r.getResponse().getHeader(HttpHeaders.SET_COOKIE);
            assertThat(setCookie).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth");
            assertThat(setCookie).doesNotContain("Max-Age"); // không "ghi nhớ" -> cookie phiên
            assertThat(r.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        }

        @Test
        @DisplayName("Ghi nhớ đăng nhập -> cookie có Max-Age")
        void rememberMeCookieIsPersistent() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            assertThat(login(email, GOOD_PASSWORD, true).getResponse().getHeader(HttpHeaders.SET_COOKIE))
                    .contains("Max-Age=");
        }

        @Test
        @DisplayName("Chống dò tài khoản: đăng ký email đã tồn tại trả về y hệt email mới")
        void registerDoesNotRevealExistingEmail() throws Exception {
            String email = newEmail();
            String first = mvc.perform(jsonPost("/api/auth/register", new Reg(email, GOOD_PASSWORD)))
                    .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            String again = mvc.perform(jsonPost("/api/auth/register", new Reg(email, "mot-mat-khau-khac-hoan-toan")))
                    .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            assertThat(again).isEqualTo(first);
            // ...và lần đăng ký thứ 2 KHÔNG ghi đè mật khẩu của chủ tài khoản
            assertThat(login(email, GOOD_PASSWORD, false).getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Email được chuẩn hoá: hoa/thường và khoảng trắng không tạo tài khoản trùng")
        void emailIsNormalized() throws Exception {
            String email = newEmail();
            register("  " + email.toUpperCase() + " ", GOOD_PASSWORD);
            assertThat(login(email, GOOD_PASSWORD, false).getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Mật khẩu yếu bị từ chối: quá ngắn, quá phổ biến, chứa email")
        void weakPasswordsRejected() throws Exception {
            for (String weak : List.of("short", "password123", "12345678", "aaaaaaaaaa")) {
                mvc.perform(jsonPost("/api/auth/register", new Reg(newEmail(), weak)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
            }
            mvc.perform(jsonPost("/api/auth/register", new Reg("nguyenvana@example.com", "nguyenvana2026")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Mật khẩu tiếng Việt có dấu được chấp nhận")
        void unicodePasswordAccepted() throws Exception {
            String email = newEmail();
            register(email, "cà phê sữa đá Hà Nội");
            assertThat(login(email, "cà phê sữa đá Hà Nội", false).getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Mass assignment: gửi kèm \"role\":\"ADMIN\" -> 400; token luôn có role CUSTOMER")
        void cannotSelfAssignRole() throws Exception {
            String email = newEmail();
            String body = "{\"email\":\"" + email + "\",\"password\":\"" + GOOD_PASSWORD + "\",\"role\":\"ADMIN\"}";
            mvc.perform(post("/api/auth/register").header("Origin", ORIGIN)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

            register(email, GOOD_PASSWORD);
            JsonNode payload = jwtPayload(accessToken(login(email, GOOD_PASSWORD, false)));
            assertThat(payload.get("roles").toString()).isEqualTo("[\"CUSTOMER\"]");
            assertThat(payload.has("email")).as("không đưa PII vào JWT").isFalse();
        }

        @Test
        @DisplayName("Mật khẩu được lưu bằng Argon2id, không phải plaintext; /me không trả hash")
        void passwordStoredAsArgon2(@Autowired vn.it4409.g16.auth.user.UserRepository users) throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            String hash = users.findByEmail(email).orElseThrow().getPasswordHash();
            assertThat(hash).startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$").doesNotContain(GOOD_PASSWORD);

            String token = accessToken(login(email, GOOD_PASSWORD, false));
            String me = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(me).contains(email).doesNotContain("argon2").doesNotContainIgnoringCase("password\"");
        }
    }

    @Nested
    @DisplayName("Đăng nhập: brute force & dò tài khoản")
    class Login_ {

        @Test
        @DisplayName("Sai email và sai mật khẩu trả về CÙNG một phản hồi")
        void sameErrorForUnknownEmailAndWrongPassword() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            MvcResult wrongPw = login(email, "sai-mat-khau-roi-nhe", false);
            MvcResult noUser = login(newEmail(), "sai-mat-khau-roi-nhe", false);
            assertThat(wrongPw.getResponse().getStatus()).isEqualTo(401);
            assertThat(noUser.getResponse().getStatus()).isEqualTo(401);
            assertThat(wrongPw.getResponse().getContentAsString()).isEqualTo(noUser.getResponse().getContentAsString());
        }

        @Test
        @DisplayName("Sai 5 lần -> khoá tạm thời, kể cả nhập đúng; hết hạn khoá thì vào lại được")
        void lockoutAfterRepeatedFailures() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            for (int i = 0; i < 5; i++) login(email, "doan-mo-lan-" + i, false);

            MvcResult locked = login(email, GOOD_PASSWORD, false);
            assertThat(locked.getResponse().getStatus()).isEqualTo(429);
            assertThat(locked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();

            clock.advance(Duration.ofSeconds(61));
            assertThat(login(email, GOOD_PASSWORD, false).getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Email KHÔNG tồn tại cũng bị khoá như email thật (không lộ qua 429)")
        void lockoutAlsoAppliesToUnknownEmails() throws Exception {
            String ghost = newEmail();
            for (int i = 0; i < 5; i++) login(ghost, "doan-mo-lan-" + i, false);
            assertThat(login(ghost, "doan-mo-lan-6", false).getResponse().getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("Rate limit theo IP: vượt ngưỡng -> 429 + Retry-After")
        void rateLimiterBlocksBursts() {
            RateLimiter limiter = new RateLimiter(clock);
            for (int i = 0; i < 3; i++) limiter.check("login", "10.0.0.1", 3, Duration.ofMinutes(1));
            try {
                limiter.check("login", "10.0.0.1", 3, Duration.ofMinutes(1));
                throw new AssertionError("lẽ ra phải bị chặn");
            } catch (ApiException e) {
                assertThat(e.status().value()).isEqualTo(429);
                assertThat(e.retryAfterSeconds()).isPositive();
            }
            limiter.check("login", "10.0.0.2", 3, Duration.ofMinutes(1)); // IP khác không bị ảnh hưởng
        }
    }

    @Nested
    @DisplayName("JWT: giả mạo token")
    class JwtForgery {

        String sub;
        String validToken;

        void setUp() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            validToken = accessToken(login(email, GOOD_PASSWORD, false));
            sub = jwtPayload(validToken).get("sub").asText();
        }

        JWTClaimsSet claims(String role) {
            Instant now = clock.instant();
            return new JWTClaimsSet.Builder().subject(sub).issuer("g16-auth-service").audience("g16-api")
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(600)))
                    .claim("roles", List.of(role)).build();
        }

        int callMe(String token) throws Exception {
            return mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getStatus();
        }

        @Test
        @DisplayName("Token hợp lệ -> 200 (đối chứng)")
        void validTokenWorks() throws Exception {
            setUp();
            assertThat(callMe(validToken)).isEqualTo(200);
        }

        @Test
        @DisplayName("alg=none (bỏ chữ ký) -> 401")
        void algNoneRejected() throws Exception {
            setUp();
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = b64.encodeToString(claims("ADMIN").toString().getBytes(StandardCharsets.UTF_8));
            assertThat(callMe(header + "." + payload + ".")).isEqualTo(401);
        }

        @Test
        @DisplayName("Nhầm thuật toán: ký HS256 bằng chính public key -> 401")
        void algorithmConfusionRejected() throws Exception {
            setUp();
            SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims("ADMIN"));
            forged.sign(new MACSigner(rsaKey.toRSAPublicKey().getEncoded()));
            assertThat(callMe(forged.serialize())).isEqualTo(401);
        }

        @Test
        @DisplayName("Sửa payload (CUSTOMER -> ADMIN) nhưng giữ chữ ký cũ -> 401")
        void tamperedPayloadRejected() throws Exception {
            setUp();
            String[] parts = validToken.split("\\.");
            String evil = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    claims("ADMIN").toString().getBytes(StandardCharsets.UTF_8));
            assertThat(callMe(parts[0] + "." + evil + "." + parts[2])).isEqualTo(401);
        }

        @Test
        @DisplayName("Ký bằng khoá RSA khác (của kẻ tấn công) -> 401")
        void foreignKeyRejected() throws Exception {
            setUp();
            RSAKey attacker = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID(rsaKey.getKeyID()).generate();
            SignedJWT forged = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), claims("ADMIN"));
            forged.sign(new RSASSASigner(attacker));
            assertThat(callMe(forged.serialize())).isEqualTo(401);
        }

        @Test
        @DisplayName("Sai audience (token dành cho hệ thống khác) -> 401")
        void wrongAudienceRejected() throws Exception {
            setUp();
            JWTClaimsSet c = new JWTClaimsSet.Builder(claims("CUSTOMER")).audience("other-app").build();
            SignedJWT t = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), c);
            t.sign(new RSASSASigner(rsaKey));
            assertThat(callMe(t.serialize())).isEqualTo(401);
        }

        @Test
        @DisplayName("JWKS chỉ công bố public key, không bao giờ lộ private key")
        void jwksNeverLeaksPrivateKey() throws Exception {
            JsonNode key = json.readTree(mvc.perform(get("/.well-known/jwks.json"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("keys").get(0);
            assertThat(key.get("kty").asText()).isEqualTo("RSA");
            assertThat(key.has("kid")).isTrue();
            for (String secret : List.of("d", "p", "q", "dp", "dq", "qi")) {
                assertThat(key.has(secret)).as("JWKS lộ tham số bí mật " + secret).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Refresh token: xoay vòng & phát hiện đánh cắp")
    class RefreshRotation {

        @Test
        @DisplayName("Refresh cấp token mới; dùng lại token cũ sau thời gian ân hạn -> thu hồi CẢ phiên")
        void reuseOfRotatedTokenRevokesFamily() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            Cookie c1 = refreshCookie(login(email, GOOD_PASSWORD, false));

            MvcResult r2 = refresh(c1);
            assertThat(r2.getResponse().getStatus()).isEqualTo(200);
            Cookie c2 = refreshCookie(r2);
            assertThat(c2.getValue()).isNotEqualTo(c1.getValue());

            // 2 tab refresh gần như cùng lúc -> 409 "thử lại", KHÔNG đăng xuất người dùng
            assertThat(refresh(c1).getResponse().getStatus()).isEqualTo(409);
            assertThat(refresh(c2).getResponse().getStatus()).as("phiên vẫn còn sống").isEqualTo(200);

            // Kẻ trộm dùng lại token cũ sau đó -> bị phát hiện
            clock.advance(Duration.ofSeconds(30));
            assertThat(refresh(c1).getResponse().getStatus()).isEqualTo(401);
            // ...và toàn bộ phiên bị thu hồi, kể cả token mới nhất mà người dùng thật đang giữ
            assertThat(refresh(c2).getResponse().getStatus()).isEqualTo(401);
            String sub = jwtPayload(accessToken(r2)).get("sub").asText();
            assertThat(auditLog.findByUserIdOrderByIdAsc(UUID.fromString(sub)))
                    .anyMatch(e -> e.getEvent().equals("REFRESH_REUSE_DETECTED"));
        }

        @Test
        @DisplayName("Token rác / không có cookie -> 401 và cookie bị xoá")
        void garbageTokenRejected() throws Exception {
            MvcResult r = refresh(new Cookie("refresh_token", "khong-phai-token-that"));
            assertThat(r.getResponse().getStatus()).isEqualTo(401);
            assertThat(r.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
            mvc.perform(post("/api/auth/refresh").header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Hết hạn tuyệt đối của phiên -> refresh liên tục cũng không gia hạn được")
        void rotationDoesNotExtendSession() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            Cookie c = refreshCookie(login(email, GOOD_PASSWORD, false)); // phiên 1 ngày
            for (int i = 0; i < 3; i++) {
                clock.advance(Duration.ofHours(7));
                MvcResult r = refresh(c);
                assertThat(r.getResponse().getStatus()).isEqualTo(200);
                c = refreshCookie(r);
            }
            clock.advance(Duration.ofHours(4)); // tổng 25 giờ
            assertThat(refresh(c).getResponse().getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Đăng xuất -> refresh token không dùng được nữa, cookie bị xoá")
        void logoutRevokes() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            Cookie c = refreshCookie(login(email, GOOD_PASSWORD, true));
            mvc.perform(post("/api/auth/logout").header("Origin", ORIGIN).cookie(c))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
            assertThat(refresh(c).getResponse().getStatus()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("Tài khoản: đổi mật khẩu, phiên, IDOR")
    class Account {

        @Test
        @DisplayName("Đổi mật khẩu -> mọi phiên bị thu hồi, access token cũ bị từ chối ngay")
        void changePasswordRevokesEverything() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            MvcResult laptop = login(email, GOOD_PASSWORD, false);
            MvcResult phone = login(email, GOOD_PASSWORD, true);
            String token = accessToken(laptop);

            clock.advance(Duration.ofSeconds(2));
            mvc.perform(jsonPost("/api/auth/change-password", new ChangePw(GOOD_PASSWORD, "mat-khau-moi-rat-dai-2026"))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_REVOKED"));
            assertThat(refresh(refreshCookie(phone)).getResponse().getStatus()).isEqualTo(401);
            assertThat(login(email, GOOD_PASSWORD, false).getResponse().getStatus()).isEqualTo(401);
            assertThat(login(email, "mat-khau-moi-rat-dai-2026", false).getResponse().getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Đổi mật khẩu cần đúng mật khẩu hiện tại")
        void changePasswordNeedsCurrentPassword() throws Exception {
            String email = newEmail();
            register(email, GOOD_PASSWORD);
            String token = accessToken(login(email, GOOD_PASSWORD, false));
            mvc.perform(jsonPost("/api/auth/change-password", new ChangePw("doan-bua-thoi", "mat-khau-moi-rat-dai-2026"))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        }

        @Test
        @DisplayName("IDOR: người dùng B không thu hồi được phiên của A (404, không phải 403)")
        void cannotRevokeOtherUsersSession() throws Exception {
            String a = newEmail();
            String b = newEmail();
            register(a, GOOD_PASSWORD);
            register(b, GOOD_PASSWORD);
            MvcResult aLogin = login(a, GOOD_PASSWORD, false);
            String aToken = accessToken(aLogin);
            String bToken = accessToken(login(b, GOOD_PASSWORD, false));

            JsonNode sessions = json.readTree(mvc.perform(get("/api/auth/sessions")
                            .header("Authorization", "Bearer " + aToken).cookie(refreshCookie(aLogin)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertThat(sessions.get(0).get("current").asBoolean()).isTrue();
            String aSessionId = sessions.get(0).get("id").asText();

            mvc.perform(delete("/api/auth/sessions/" + aSessionId).header("Origin", ORIGIN)
                            .header("Authorization", "Bearer " + bToken))
                    .andExpect(status().isNotFound());
            assertThat(refresh(refreshCookie(aLogin)).getResponse().getStatus()).as("phiên của A vẫn sống").isEqualTo(200);

            mvc.perform(delete("/api/auth/sessions/" + aSessionId).header("Origin", ORIGIN)
                            .header("Authorization", "Bearer " + aToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Endpoint cần đăng nhập mà không có token -> 401")
        void protectedEndpointsNeedToken() throws Exception {
            mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/auth/sessions")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/auth/logout-all").header("Origin", ORIGIN)).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CSRF & request lạ")
    class Csrf {

        @Test
        @DisplayName("POST từ Origin lạ (trang độc) -> 403")
        void foreignOriginRejected() throws Exception {
            mvc.perform(post("/api/auth/login").header("Origin", "https://evil.example")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.c\",\"password\":\"x\",\"rememberMe\":false}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORIGIN_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Form HTML (x-www-form-urlencoded) -> 415, không xử lý")
        void htmlFormRejected() throws Exception {
            mvc.perform(post("/api/auth/login").header("Origin", ORIGIN)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED).content("email=a@b.c&password=x"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Body quá lớn -> 413")
        void hugeBodyRejected() throws Exception {
            String huge = "{\"email\":\"a@b.c\",\"password\":\"" + "x".repeat(20_000) + "\"}";
            mvc.perform(post("/api/auth/login").header("Origin", ORIGIN)
                            .contentType(MediaType.APPLICATION_JSON).content(huge))
                    .andExpect(status().isPayloadTooLarge());
        }

        @Test
        @DisplayName("Header bảo mật có mặt; lỗi không lộ stack trace")
        void securityHeaders() throws Exception {
            MvcResult r = mvc.perform(get("/api/auth/me")).andReturn();
            assertThat(r.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(r.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
            assertThat(r.getResponse().getHeader("Content-Security-Policy")).contains("default-src 'none'");

            String body = mvc.perform(post("/api/auth/login").header("Origin", ORIGIN)
                            .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                    .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("Exception", "at com.", "at org.");
        }
    }
}
