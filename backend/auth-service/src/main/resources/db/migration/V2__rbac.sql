-- Auth Service schema v2: phân quyền RBAC (Role-Based Access Control).
--
--   users (n) ──── (1) roles (1) ──── (n) role_permissions (n) ──── (1) permissions
--
-- Mỗi tài khoản gắn với ĐÚNG MỘT role (users.role). Mỗi role có một tập permission.
-- Service khác chỉ kiểm PERMISSION (vd "product:write"), không kiểm tên role, nên muốn đổi quyền
-- của một role chỉ cần viết migration mới ở đây; code của Product/Order... không phải sửa.
--
-- Không có API để sửa bảng roles/permissions/role_permissions: thay đổi quyền là thay đổi bảo mật,
-- phải đi qua Pull Request + review (file migration mới), không phải một cú click trên web admin.

CREATE TABLE roles (
    code        VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT ck_roles_code CHECK (code = UPPER(code))
);

CREATE TABLE permissions (
    -- Định dạng "tài_nguyên:hành_động". Bắt buộc có dấu ":" để không bao giờ trùng với
    -- authority dạng "ROLE_xxx" của Spring Security (chống leo quyền do nhầm tên).
    code        VARCHAR(50)  PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT ck_permissions_code CHECK (code LIKE '%:%')
);

CREATE TABLE role_permissions (
    role_code       VARCHAR(20) NOT NULL REFERENCES roles (code) ON DELETE CASCADE,
    permission_code VARCHAR(50) NOT NULL REFERENCES permissions (code) ON DELETE CASCADE,
    PRIMARY KEY (role_code, permission_code)
);

-- ------------------------------------------------------------------ dữ liệu khởi tạo

INSERT INTO roles (code, name, description) VALUES
    ('CUSTOMER', 'Khách hàng',    'Mặc định khi đăng ký. Mua hàng, quản lý hồ sơ và đơn hàng của chính mình.'),
    ('ADMIN',    'Quản trị viên', 'Toàn quyền: quản lý sản phẩm, đơn hàng, tài khoản và phân quyền.');
-- Cần thêm role (vd nhân viên) sau này: viết migration mới V3__... thêm vào roles + role_permissions.

INSERT INTO permissions (code, description) VALUES
    -- Khách hàng
    ('order:create',        'Đặt hàng'),
    ('review:write',        'Viết đánh giá sản phẩm'),
    -- Quản trị: vận hành cửa hàng
    ('product:write',       'Thêm, sửa, xoá sản phẩm và tồn kho'),
    ('order:read:any',      'Xem đơn hàng của mọi khách'),
    ('order:update-status', 'Cập nhật trạng thái đơn hàng'),
    ('profile:read:any',    'Xem hồ sơ khách hàng (số điện thoại đã che)'),
    -- Quản trị
    ('account:read',        'Xem danh sách tài khoản (email, role, trạng thái)'),
    ('account:lock',        'Khoá / mở khoá tài khoản'),
    ('role:read',           'Xem danh sách role và quyền'),
    ('role:assign',         'Đổi role của tài khoản khác');

-- Ghi chú: thao tác trên dữ liệu CỦA CHÍNH MÌNH (xem/sửa hồ sơ, sổ địa chỉ, đơn của mình, đổi mật khẩu)
-- không cần permission: chỉ cần đăng nhập + kiểm tra chủ sở hữu (userId lấy từ JWT).
INSERT INTO role_permissions (role_code, permission_code) VALUES
    ('CUSTOMER', 'order:create'),
    ('CUSTOMER', 'review:write'),

    ('ADMIN', 'order:create'),
    ('ADMIN', 'review:write'),
    ('ADMIN', 'product:write'),
    ('ADMIN', 'order:read:any'),
    ('ADMIN', 'order:update-status'),
    ('ADMIN', 'profile:read:any'),
    ('ADMIN', 'account:read'),
    ('ADMIN', 'account:lock'),
    ('ADMIN', 'role:read'),
    ('ADMIN', 'role:assign');

-- ------------------------------------------------------------------ gắn users vào roles

-- Trước đây role bị "đóng cứng" bằng CHECK ('CUSTOMER','ADMIN'). Nay thay bằng khoá ngoại:
-- role phải tồn tại trong bảng roles; không xoá được role khi còn tài khoản đang dùng nó.
ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ADD CONSTRAINT fk_users_role FOREIGN KEY (role) REFERENCES roles (code);
CREATE INDEX idx_users_role ON users (role);

-- Mốc thay đổi quyền gần nhất (đổi role, khoá/mở khoá). Access token có iat trước mốc này bị Auth
-- Service từ chối ngay; các service khác từ chối khi token hết hạn (tối đa 15 phút).
ALTER TABLE users ADD COLUMN authz_changed_at TIMESTAMP WITH TIME ZONE;
