# User Service

Service quản lý hồ sơ và sổ địa chỉ của dự án IT4409 Nhóm 16, chạy ở cổng **8082**. Viết bằng Spring Boot 3.5 và PostgreSQL.

Service này **không đăng nhập ai**. Nó chỉ kiểm access token do [Auth Service](../auth-service/README.md) cấp, bằng public key lấy từ JWKS.
Luồng xử lý và thiết kế chi tiết nằm trong [../docs/MO_TA_HE_THONG_Auth_User.md](../docs/MO_TA_HE_THONG_Auth_User.md).

## Chạy
```bash
# Cần Auth Service chạy ở :8081 trước (để lấy JWKS)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local    # H2, khoá PII dành cho dev
./mvnw test                                                 # 14 bài tự tấn công
```
Khi chạy bằng Docker: đặt `USER_DB_PASSWORD` và `USER_PII_KEY` (tạo bằng `openssl rand -base64 32`) trong `.env` ở thư mục gốc repo, rồi chạy `docker compose up -d --build`.

## API
| Method | Path | Mô tả |
|---|---|---|
| GET / PUT | `/api/users/me` | Xem và sửa hồ sơ (`fullName`, `phone`, `dob`) |
| GET / POST | `/api/users/me/addresses` | Danh sách và thêm địa chỉ (tối đa 10) |
| PUT / DELETE | `/api/users/me/addresses/{id}` | Sửa và xoá |
| PATCH | `/api/users/me/addresses/{id}/default` | Đặt làm địa chỉ mặc định |
| GET | `/api/admin/users?page&size` | Chỉ ADMIN; số điện thoại đã che, tối đa 50 dòng mỗi trang |

## Bảo mật
| Mối đe doạ | Cách chống | File |
|---|---|---|
| IDOR/BOLA | `userId` lấy từ `jwt.sub`; repository chỉ có `findByIdAndUserId`; trả 404 | `security/CurrentUser`, `address/AddressRepository` |
| Mass assignment | DTO record, `fail-on-unknown-properties` | `web/dto/*Request` |
| XSS lưu trữ, dữ liệu rác | Allowlist regex, chuẩn hoá trước khi validate | `web/dto/Inputs` |
| Lộ PII khi lộ DB | AES-256-GCM qua JPA converter, IV ngẫu nhiên | `crypto/PiiCipher`, `crypto/EncryptedStringConverter` |
| Vượt giới hạn 10 địa chỉ do race | `SELECT … FOR UPDATE` trên hồ sơ | `ProfileRepository.lockById` |
| Hai địa chỉ mặc định | Logic trong service cộng partial unique index trên PostgreSQL | `AddressService`, `db/vendor/postgresql/V2__…` |
| Ghi đè lẫn nhau khi sửa cùng lúc | Optimistic lock bằng `@Version`, trả 409 | `Profile`, `Address` |
| Leo thang quyền | `/api/admin/**` kiểm theo URL và `@PreAuthorize` | `config/SecurityConfig`, `web/UserController` |
| Giả mạo JWT | Chỉ RS256, kiểm iss, aud, exp | `config/SecurityConfig` |
