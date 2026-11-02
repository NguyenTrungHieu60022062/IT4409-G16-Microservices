import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import Account from "./pages/Account.jsx";
import CartService from "./pages/CartService.jsx";
import Login from "./pages/Login.jsx";
import Notification from "./pages/Notification.jsx";
import Order from "./pages/Order.jsx";
import Payment from "./pages/Payment.jsx";
import ProductInventory from "./pages/ProductInventory.jsx";
import Register from "./pages/Register.jsx";
import Shipping from "./pages/Shipping.jsx";

const navigation = [
  { to: "/account", label: "Tài khoản" },
  { to: "/cart", label: "Giỏ hàng" },
  { to: "/orders", label: "Đơn hàng" },
  { to: "/products", label: "Sản phẩm" },
  { to: "/payments", label: "Thanh toán" },
  { to: "/shipping", label: "Vận chuyển" },
  { to: "/notifications", label: "Thông báo" },
  { to: "/login", label: "Đăng nhập" },
  { to: "/register", label: "Đăng ký" },
];

function AppLayout() {
  return (
    <>
      <header className="app-header">
        <NavLink className="app-brand" to="/account">Sprylo</NavLink>
        <nav className="app-navigation" aria-label="Điều hướng chức năng">
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => isActive ? "app-nav-link active" : "app-nav-link"}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <Routes>
        <Route path="/" element={<Navigate to="/account" replace />} />
        <Route path="/account" element={<Account />} />
        <Route path="/cart" element={<CartService />} />
        <Route path="/orders" element={<Order />} />
        <Route path="/products" element={<ProductInventory />} />
        <Route path="/payments" element={<Payment />} />
        <Route path="/shipping" element={<Shipping />} />
        <Route path="/notifications" element={<Notification />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  );
}

function NotFound() {
  return (
    <main className="not-found">
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn này chưa có chức năng tương ứng trong project hiện tại.</p>
      <NavLink to="/account">Quay về trang tài khoản</NavLink>
    </main>
  );
}

export default function App() {
  return <AppLayout />;
}
