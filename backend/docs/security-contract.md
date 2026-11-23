# Hợp đồng bảo mật (Security Contract): dùng chung cho mọi service

Tài liệu này dành cho các bạn làm Product, Cart, Order, Payment, Shipping và Notification. Nó mô tả cách service của bạn biết **ai đang gọi** và **người đó có quyền gì**.

## 1. Access token
Frontend gửi token trong header của mọi request cần đăng nhập:
```
Authorization: Bearer <access token>
```
Access token là **JWT ký RS256**, sống **15 phút**. Payload mẫu:
```json
{
  "iss": "g16-auth-service",
  "aud": ["g16-api"],
  "sub": "4b8f0c1e-6a0e-4a57-9d1c-2f1f0f6b9a10",
  "roles": ["CUSTOMER"],
  "perms": ["order:create", "review:write"],
  "iat": 1762650000,
  "exp": 1762650900,
  "jti": "..."
}
```

| Claim | Ý nghĩa | Service của bạn dùng thế nào |
|---|---|---|
| `sub` | **userId** (UUID) | Đây là định danh duy nhất của người dùng. Lưu `user_id` trong bảng của bạn bằng giá trị này |
| `roles` | Đúng **một** role: `CUSTOMER` hoặc `ADMIN` | Chỉ để hiển thị. **Đừng** phân quyền theo tên role |
| `perms` | Danh sách quyền của role đó | **Phân quyền theo claim này**: `hasAuthority('product:write')` |
| `iss`, `aud` | Ai cấp token, token dành cho ai | Bắt buộc kiểm tra (đã có trong cấu hình bên dưới) |

Token **không chứa** email hay số điện thoại, vì ai cầm token cũng đọc được payload. Cần email thì gọi `GET /api/auth/me`.

## 1b. Phân quyền RBAC: role và permission
Mỗi tài khoản gắn với **đúng một role**, mỗi role có một tập **permission** (lưu trong DB của Auth: bảng `roles`, `permissions`, `role_permissions`). Auth đưa permission vào claim `perms` mỗi lần cấp token.

| Permission | CUSTOMER | ADMIN | Service dùng |
|---|:-:|:-:|---|
| `order:create` | ✔ | ✔ | Order |
| `review:write` | ✔ | ✔ | Product / Review |
| `product:write` | | ✔ | Product |
| `order:read:any` | | ✔ | Order |
| `order:update-status` | | ✔ | Order, Shipping |
| `profile:read:any` | | ✔ | User |
| `account:read`, `account:lock` | | ✔ | Auth |
| `role:read`, `role:assign` | | ✔ | Auth |

- Thao tác trên **dữ liệu của chính mình** (giỏ hàng, đơn của mình, hồ sơ của mình) **không cần permission**: chỉ cần đăng nhập và kiểm chủ sở hữu (quy tắc 1 và 2 bên dưới).
- Kiểm **permission**, không kiểm tên role: `@PreAuthorize("hasAuthority('product:write')")`, không viết `hasRole('ADMIN')`. Khi nhóm quyết định cho thêm role nào làm việc gì, chỉ cần một migration bên Auth; service của bạn không phải sửa.
- Cần permission mới (vd `coupon:write`)? Nhắn người làm Auth thêm vào migration, đặt tên dạng `tài_nguyên:hành_động`.
- Bị đổi role hoặc bị khoá: Auth thu hồi refresh token ngay. Access token cũ vẫn dùng được ở service khác **tối đa 15 phút** (đến khi hết hạn). Với thao tác cực kỳ nhạy cảm, gọi lại `GET /api/auth/me` để kiểm chắc.

## 2. Cấu hình Spring Boot (copy vào service của bạn)
`pom.xml`:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```
`application.yml`:
```yaml
auth:
  jwks-uri: ${AUTH_JWKS_URI:http://auth-service:8081/.well-known/jwks.json}   # tên service trong Docker network
```
`SecurityConfig.java`:
```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()   // ví dụ: API công khai
            .anyRequest().authenticated())                                     // mặc định: cần đăng nhập
        .oauth2ResourceServer(rs -> rs.jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter())));
    return http.build();
  }

  /** "roles" -> ROLE_ADMIN ; "perms" -> product:write (dùng với hasAuthority) */
  @Bean
  JwtAuthenticationConverter jwtAuthConverter() {
    var roles = new JwtGrantedAuthoritiesConverter();
    roles.setAuthoritiesClaimName("roles");
    roles.setAuthorityPrefix("ROLE_");
    var perms = new JwtGrantedAuthoritiesConverter();
    perms.setAuthoritiesClaimName("perms");
    perms.setAuthorityPrefix("");
    var c = new JwtAuthenticationConverter();
    c.setJwtGrantedAuthoritiesConverter(jwt -> {
      List<GrantedAuthority> all = new ArrayList<>(roles.convert(jwt));
      all.addAll(perms.convert(jwt));
      return all;
    });
    return c;
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${auth.jwks-uri}") String jwksUri) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
        .jwsAlgorithm(SignatureAlgorithm.RS256)          // chỉ RS256: chặn alg=none / HS256
        .build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer("g16-auth-service"),   // exp + iss
        new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("g16-api"))));
    return decoder;
  }
}
```
Decoder tự tải và cache public key từ JWKS. Service của bạn **không cần** gọi Auth Service ở mỗi request.
Lấy userId trong controller:
```java
@GetMapping("/api/orders")
List<OrderDto> myOrders(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return orderService.findByUser(userId);
}

@PreAuthorize("hasAuthority('order:read:any')")   // hiện chỉ ADMIN có quyền này
@GetMapping("/api/admin/orders")
List<OrderDto> allOrders() { ... }
```

## 3. Năm quy tắc bắt buộc
1. **userId luôn lấy từ `jwt.getSubject()`**, không bao giờ lấy từ URL, query hay body. Ví dụ `GET /api/orders?userId=...` là sai, vì ai cũng sửa được tham số đó để xem đơn của người khác. Lỗi này gọi là **IDOR/BOLA**, đứng số 1 trong OWASP API Top 10.
2. **Truy vấn luôn kèm điều kiện chủ sở hữu**: dùng `findByIdAndUserId(id, userId)`, không dùng `findById(id)`. Nếu không tìm thấy thì trả **404**, không trả 403, để kẻ dò không biết id đó có tồn tại.
3. **Không bind thẳng request vào Entity.** Dùng DTO riêng chỉ chứa các trường được phép sửa, để client không gửi được kèm `"status":"PAID"` hay `"price":0`.
4. **Mặc định từ chối** (`anyRequest().authenticated()`). API công khai phải khai báo rõ ràng.
5. **Không log token, mật khẩu hay số thẻ.**

## 4. Endpoint của Auth Service
| Method | Path | Xác thực | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | — | `{email, password}`, luôn trả 202 |
| POST | `/api/auth/login` | — | `{email, password, rememberMe}`, trả `{accessToken, expiresIn}` và cookie refresh |
| POST | `/api/auth/refresh` | cookie | Cấp access token mới, xoay refresh token |
| POST | `/api/auth/logout` | cookie | Thu hồi phiên hiện tại |
| POST | `/api/auth/logout-all` | Bearer | Thu hồi mọi phiên |
| GET | `/api/auth/me` | Bearer | `{id, email, role, permissions, createdAt}`. Frontend dùng `permissions` để ẩn/hiện menu |
| POST | `/api/auth/change-password` | Bearer | `{currentPassword, newPassword}` |
| GET | `/api/auth/sessions` | Bearer | Danh sách thiết bị đang đăng nhập |
| DELETE | `/api/auth/sessions/{id}` | Bearer | Đăng xuất một thiết bị |
| GET | `/.well-known/jwks.json` | — | Public key để kiểm chữ ký |
| GET | `/api/admin/roles` | `role:read` | Danh sách role và permission |
| GET | `/api/admin/accounts?page&size` | `account:read` | Danh sách tài khoản (tối đa 50/trang) |
| GET | `/api/admin/accounts/{id}` | `account:read` | Một tài khoản |
| PUT | `/api/admin/accounts/{id}/role` | `role:assign` | `{role}`. Không tự đổi mình, không làm mất ADMIN cuối (409) |
| PUT | `/api/admin/accounts/{id}/status` | `account:lock` | `{status: ACTIVE\|DISABLED}`. Khoá thì thu hồi mọi phiên |

Mã lỗi trả về luôn có dạng `{"code": "...", "message": "...", "details": [...]}`.

## 5. Endpoint của User Service (:8082)
| Method | Path | Mô tả |
|---|---|---|
| GET / PUT | `/api/users/me` | Hồ sơ của người đang đăng nhập |
| GET | `/api/admin/users?page&size` | Danh sách hồ sơ (số điện thoại đã che), cần `profile:read:any` |
| GET / POST / PUT / DELETE / PATCH | `/api/users/me/addresses[/{id}[/default]]` | Sổ địa chỉ |

Order và Shipping cần địa chỉ giao hàng thì nên **chép (snapshot)** địa chỉ vào đơn hàng tại thời điểm đặt, đừng tham chiếu `addressId`. Người dùng có thể sửa hoặc xoá địa chỉ sau đó, và đơn cũ không được thay đổi theo. Endpoint nội bộ cho service khác sẽ được bổ sung khi các bạn cần.

Tài liệu đầy đủ: [MO_TA_HE_THONG_Auth_User.md](MO_TA_HE_THONG_Auth_User.md).
