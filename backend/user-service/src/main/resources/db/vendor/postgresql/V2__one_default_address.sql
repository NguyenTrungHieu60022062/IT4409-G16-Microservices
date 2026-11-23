-- Chỉ chạy trên PostgreSQL (H2 không hỗ trợ partial index).
-- Ràng buộc ở tầng DB: mỗi người dùng có TỐI ĐA MỘT địa chỉ mặc định. Dù code có bug hay 2 request
-- chạy song song, database vẫn không cho phép trạng thái sai.
CREATE UNIQUE INDEX uq_addresses_one_default ON addresses (user_id) WHERE is_default;
