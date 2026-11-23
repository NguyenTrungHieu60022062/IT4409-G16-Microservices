# Auth Service

Service đăng ký, đăng nhập, cấp JWT và quản lý phiên của dự án IT4409 Nhóm 16. Viết bằng Spring Boot 3.5, Java 21 và PostgreSQL.
Các service khác tích hợp theo hướng dẫn trong [../docs/security-contract.md](../docs/security-contract.md).

## Chạy

**Cách 1: Docker (đầy đủ, có PostgreSQL)**. Chạy ở thư mục gốc repo:
```bash
cp .env.example .env        # rồi sửa AUTH_DB_PASSWORD
docker compose up -d --build
curl http://localhost:8081/actuator/health
```

**Cách 2: chạy thử không cần Docker** (dùng H2, dữ liệu lưu trong `./data/`):
```bash
cd backend/auth-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local      # Windows: mvnw.cmd ...
```

**Chạy test bảo mật** (31 bài "tự tấn công"):
```bash
./mvnw test
```

Frontend gọi service này qua `VITE_AUTH_API_URL`, mặc định là `http://localhost:8081`.

## Các cơ chế bảo mật và vị trí trong code

| Mối đe doạ | Cách chống | File |
|---|---|---|
| Lộ DB làm lộ mật khẩu | Argon2id (m=19 MiB, t=2, p=1), salt ngẫu nhiên, tự băm lại khi tham số đổi | `config/SecurityConfig.java`, `AuthService.login` |
| Mật khẩu yếu | 8–128 ký tự, chặn mật khẩu phổ biến và mật khẩu chứa email, chuẩn hoá Unicode NFKC (NIST 800-63B) | `security/PasswordPolicy.java` |
| Brute force một tài khoản | Khoá tạm 1 phút, mỗi lần sau nhân đôi, tối đa 15 phút | `security/LoginAttemptService.java` |
| Một IP thử nhiều tài khoản | Rate limit theo IP, trả 429 kèm `Retry-After` | `security/RateLimiter.java` |
| Dò xem email nào có tài khoản | Cùng thông điệp, cùng thời gian xử lý (hash giả); register luôn trả 202; khoá cả email không tồn tại | `AuthService.register/login` |
| Giả mạo JWT | RS256, chỉ chấp nhận đúng thuật toán đó; kiểm `iss`, `aud`, `exp` | `config/JwtKeyConfig.java` |
| Lộ private key qua JWKS | Chỉ công bố `toPublicJWK()` | `web/JwksController.java` |
| XSS lấy token | Access token chỉ lưu trong bộ nhớ JS; refresh token nằm trong cookie HttpOnly | `web/AuthController.refreshCookie` |
| Refresh token bị đánh cắp | Xoay vòng token, phát hiện tái sử dụng (thu hồi cả family), hạn tuyệt đối không gia hạn được | `token/RefreshTokenService.java` |
| Race condition khi refresh | `UPDATE … WHERE revoked_at IS NULL` nguyên tử, có thời gian ân hạn 10 giây cho trường hợp nhiều tab | `token/RefreshTokenRepository.markRotated` |
| CSRF / login CSRF | Cookie `SameSite=Strict`, kiểm `Origin`, chỉ nhận `application/json` | `security/RequestGuardFilter.java` |
| Token cũ sau khi đổi mật khẩu | Thu hồi mọi phiên; token có `iat` trước thời điểm đổi mật khẩu bị từ chối | `AuthService.requireActiveUser` |
| IDOR khi thu hồi phiên | Điều kiện `user_id` nằm ngay trong câu UPDATE, trả 404 nếu không thuộc về mình | `RefreshTokenRepository.revokeFamilyOfUser` |
| Mass assignment (`"role":"ADMIN"`) | DTO record cùng `fail-on-unknown-properties` | `web/dto/*`, `application.yml` |
| Lộ thông tin qua lỗi | JSON lỗi gọn, không có stack trace | `web/ApiExceptionHandler.java` |
| Log injection, lộ PII trong log | Bỏ ký tự điều khiển, che bớt email, không log mật khẩu hay token | `security/ClientInfo.java`, `audit/AuditService.java` |
| DoS bằng body lớn | Giới hạn body 16 KB, giới hạn độ dài trường | `RequestGuardFilter`, DTO `@Size` |

## Tự tấn công bằng tay (ngoài test tự động)
```bash
B=http://localhost:8081; O="Origin: http://localhost:5173"
# 1. Brute force: lần thứ 6 phải nhận 429
for i in $(seq 1 7); do curl -s -o /dev/null -w "%{http_code}\n" -X POST $B/api/auth/login -H "$O" \
  -H "Content-Type: application/json" -d '{"email":"demo@sprylo.vn","password":"doan-'$i'"}'; done
# 2. CSRF: Origin lạ phải nhận 403
curl -i -X POST $B/api/auth/logout -H "Origin: https://evil.example"
# 3. Mass assignment: phải nhận 400
curl -X POST $B/api/auth/register -H "$O" -H "Content-Type: application/json" \
  -d '{"email":"x@y.vn","password":"ca-phe-sang-2026","role":"ADMIN"}'
# 4. JWT: dán token vào jwt.io, sửa roles thành ADMIN rồi gọi /api/auth/me, phải nhận 401
```

## Việc tiếp theo
- Xác thực email và quên mật khẩu (token một lần, hết hạn sau 15 phút) khi Notification Service sẵn sàng.
- Chuyển rate limit và lockout sang Redis khi chạy nhiều instance.
- Khoá ký JWT cố định qua Docker secret (xem phần `secrets` trong `docker-compose.yml`), sau đó là xoay khoá theo `kid`.
- 2FA TOTP (tuỳ chọn).
