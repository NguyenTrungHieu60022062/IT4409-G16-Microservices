-- User Service schema v1 (DB riêng "users", tách khỏi DB của Auth Service).
-- user_id = claim "sub" trong JWT do Auth Service cấp. Không có khoá ngoại sang bảng users của Auth
-- vì mỗi microservice sở hữu database riêng (database-per-service).

CREATE TABLE profiles (
    user_id     UUID         PRIMARY KEY,
    full_name   VARCHAR(100),
    -- Số điện thoại MÃ HOÁ AES-256-GCM ở tầng ứng dụng: "v1:<base64(iv|ciphertext|tag)>".
    -- Lộ bản backup DB cũng không đọc được nếu không có khoá USER_PII_KEY.
    phone_enc   VARCHAR(255),
    dob         DATE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Optimistic locking: 2 request sửa cùng lúc -> request sau bị từ chối thay vì ghi đè âm thầm.
    version     BIGINT       NOT NULL
);

CREATE TABLE addresses (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES profiles (user_id) ON DELETE CASCADE,
    receiver    VARCHAR(100) NOT NULL,
    phone_enc   VARCHAR(255) NOT NULL,
    line1       VARCHAR(200) NOT NULL,
    ward        VARCHAR(100) NOT NULL,
    district    VARCHAR(100) NOT NULL,
    province    VARCHAR(100) NOT NULL,
    is_default  BOOLEAN      NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT       NOT NULL
);
-- Mọi truy vấn địa chỉ đều lọc theo user_id (chống IDOR) -> cần index.
CREATE INDEX idx_addresses_user ON addresses (user_id);
