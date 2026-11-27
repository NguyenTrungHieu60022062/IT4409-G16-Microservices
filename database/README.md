# Sprylo – Database scripts (Payment, Shipping, Notification)

Bộ script này triển khai **3 database riêng** theo tài liệu thiết kế nhóm đã cung cấp:
- `payment_db`: Payment Service
- `shipping_db`: Shipping Service
- `notification_db`: Notification Service

DBMS: PostgreSQL 13+. ID dùng UUID; thời gian dùng `TIMESTAMPTZ`; tiền dùng `NUMERIC(15,2)`.
Các `order_id`, `customer_id`, `user_id` là mã tham chiếu từ service khác nên **không tạo foreign key liên database**.

## 1. Tạo database

Kết nối PostgreSQL bằng tài khoản có quyền tạo database, sau đó chạy:

```sql
CREATE DATABASE payment_db;
CREATE DATABASE shipping_db;
CREATE DATABASE notification_db;
```

## 2. Chạy schema

Có thể dùng pgAdmin hoặc `psql`.

```bash
psql -U postgres -d payment_db -f payment/payment_db.sql
psql -U postgres -d shipping_db -f shipping/shipping_db.sql
psql -U postgres -d notification_db -f notification/notification_db.sql
```

Nếu chạy từ thư mục `database/`, điều chỉnh đường dẫn theo vị trí các file SQL.

## 3. Bảng được tạo

| Database | Tables |
|---|---|
| payment_db | `payments`, `payment_events`, `payment_refunds` |
| shipping_db | `shipments`, `shipment_events` |
| notification_db | `notifications`, `notification_recipients` |

## 4. Lưu ý tích hợp backend

- Mỗi service chỉ kết nối và ghi vào database của chính nó.
- `payment_events.payment_id`, `payment_refunds.payment_id`, `shipment_events.shipment_id`,
  `notification_recipients.notification_id` là FK nội bộ database.
- `order_id`, `customer_id`, `user_id`, `created_by` không có FK vì thuộc service khác.
- Không lưu số thẻ, CVV hoặc thông tin xác thực thanh toán thô.
- Khi hoàn tiền, Payment Service cần khóa bản ghi payment trong transaction và kiểm tra tổng tiền hoàn thành công không vượt số tiền thanh toán thành công.
- Khi cập nhật trạng thái, backend nên ghi event trong cùng transaction với bản ghi chính.
- Các file hiện tại là SQL khởi tạo schema. Khi tích hợp Spring Boot + Flyway, có thể chuyển nội dung từng file thành migration `V1__init_schema.sql` trong từng service.

## 5. Kiểm tra nhanh

Sau khi chạy từng file:

```sql
-- Chạy riêng trong từng database
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;
```
