# Sprylo Frontend – React + Vite

Frontend prototype được chuyển từ các file HTML ban đầu sang React Single Page Application (SPA).

## 1. Công nghệ

- React
- Vite
- React Router DOM

## 2. Chạy project

Cài Node.js (khuyến nghị bản LTS), sau đó mở terminal tại thư mục `frontend`:

```bash
npm install
npm run dev
```

Mở URL local mà Vite hiển thị trong terminal.

Tạo bản build để kiểm tra:

```bash
npm run build
```

## 3. Cấu trúc thư mục

```text
frontend/
├── index.html
├── package.json
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── components/
│   ├── pages/
│   │   ├── Account.jsx
│   │   ├── CartService.jsx
│   │   ├── Login.jsx
│   │   ├── Notification.jsx
│   │   ├── Order.jsx
│   │   ├── Payment.jsx
│   │   ├── ProductInventory.jsx
│   │   ├── Register.jsx
│   │   └── Shipping.jsx
│   └── styles/
│       └── global.css
└── pages/                 # HTML gốc, giữ lại để đối chiếu
```

## 4. Các route SPA

| URL | Component | File HTML nguồn |
|---|---|---|
| `/account` | Account | `account.html` |
| `/cart` | CartService | `cartservice.html` |
| `/login` | Login | `login.html` |
| `/register` | Register | `register.html` |
| `/notifications` | Notification | `notification.html` |
| `/orders` | Order | `order.html` |
| `/payments` | Payment | `payment.html` |
| `/products` | ProductInventory | `product&inventory.html` |
| `/shipping` | Shipping | `shipping.html` |

Điều hướng giữa các route được React Router xử lý ở phía client, không tải lại toàn bộ document.

## 5. Quy ước code

- Tên component và file JSX: `PascalCase`.
- Tên biến và hàm: `camelCase`.
- Tên CSS class: `kebab-case`.
- CSS dùng `className`, label dùng `htmlFor`.
- Mỗi trang là một component độc lập trong `src/pages`.
- Thành phần dùng chung đặt trong `src/components`.
- CSS dùng chung đặt trong `src/styles`; CSS riêng từng trang có thể đặt tại `src/styles/pages`.
- Logic gọi API nên được tách vào `src/services`, không viết lẫn vào JSX.

## 6. Những giới hạn của file ZIP nguồn

Bản ZIP ban đầu chỉ chứa README và các file HTML. Không có file CSS, JavaScript, ảnh hoặc backend được đính kèm.

Một số HTML gốc tham chiếu tới các tài nguyên chưa có trong ZIP, ví dụ:

- `assets/css/styles.css`
- `assets/css/auth.css`
- `notification.css`, `payment.css`, `shipping.css`
- `assets/js/main.js`, `assets/js/api.js`, `assets/js/auth.js` và các file JS khác.

Vì vậy, bản chuyển đổi này giữ lại cấu trúc và nội dung HTML có trong ZIP, tạo lớp CSS nền tối thiểu và bỏ các thẻ script tham chiếu tới file JS không tồn tại. Giao diện gốc chưa thể được khôi phục chính xác cho đến khi có CSS và tài nguyên tương ứng. Các nút/form hiện vẫn chủ yếu là giao diện tĩnh; chưa tự động có nghiệp vụ/API.

Một số liên kết trong HTML gốc trỏ tới các trang không nằm trong ZIP (ví dụ `shop.html`, `product.html`, `contact.html`, `forgot-password.html`). Các liên kết tới chức năng đã có được ánh xạ sang route SPA; những chức năng không có trong nguồn cần được bổ sung sau.

## 7. Việc làm tiếp theo

1. Bổ sung CSS gốc và chuẩn hóa thành `global.css`, `components.css`, cùng CSS riêng từng trang.
2. Tách Header, Navigation, Footer, Button, Table và Form thành component dùng chung khi xác định được cấu trúc giao diện chung.
3. Bổ sung xử lý form, sự kiện và state React.
4. Tích hợp API/backend microservices khi có source hoặc đặc tả endpoint.
5. Chạy kiểm thử từng route và kiểm tra responsive.
