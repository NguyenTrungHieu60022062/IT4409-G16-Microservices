import { useEffect } from "react";
import "../styles/sprylo.css";
import "../styles/pages/auth.css";

export default function Login() {
  useEffect(() => {
    document.title = "Đăng nhập | Sprylo";
  }, []);

  return (
    <div className="sprylo">
      <a className="skip-link" href="#main">Bỏ qua tới nội dung</a>
      <div className="utility"><div className="container"><span className="promo"><span className="tag">SALE</span>Miễn phí vận chuyển cho đơn từ 500.000₫ · Đổi trả trong 30 ngày</span><span className="links"><a href="#">Tra cứu đơn hàng</a><a href="#">Trợ giúp</a><a href="#">VI · VND</a></span></div></div>
      <header className="site-header">
      <div className="container">
      <a href="/" className="brand"><span className="brand-mark">S</span> Sprylo</a>
      <form className="search" role="search" action="shop.html">
      <input type="search" name="q" maxLength="100" placeholder="Tìm sản phẩm, thương hiệu, danh mục…" aria-label="Tìm kiếm sản phẩm" />
      <button type="submit" aria-label="Tìm kiếm"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"><circle cx="11" cy="11" r="7"></circle><path d="M21 21l-4.3-4.3"></path></svg></button>
      </form>
      <div className="icon-row">
      <a href="/account" className="icon-btn" aria-label="Tài khoản"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="8" r="4"></circle><path d="M4 21c0-4 4-7 8-7s8 3 8 7"></path></svg></a>
      <a href="#" className="icon-btn" aria-label="Yêu thích"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg></a>
      <a href="/cart" className="icon-btn icon-btn--cart" aria-label="Giỏ hàng"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M6 2l-2 5v13a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7l-2-5z"></path><path d="M4 7h16"></path><path d="M16 11a4 4 0 0 1-8 0"></path></svg></a>
      <button className="nav-toggle" aria-label="Mở menu" aria-expanded="false">≡</button>
      </div>
      </div>
      </header>
      <nav className="nav-bar" aria-label="Điều hướng chính">
      <div className="container">
      <a href="/products" className="all-cats"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round"><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>Tất cả danh mục</a>
      <div className="main-nav">
      <a href="/">Trang chủ</a><a href="/products">Cửa hàng</a>
      <a href="/products">Sản phẩm</a><a href="/cart">Giỏ hàng</a>
      <a href="/">Liên hệ</a>
      </div>
      <span className="nav-cta">GIẢM ĐẾN <strong>60%</strong> MỌI SẢN PHẨM</span>
      </div>
      </nav>
      <div className="drawer" id="drawer" aria-hidden="true">
      <div className="drawer-head"><a href="/" className="brand"><span className="brand-mark">S</span> Sprylo</a><button className="drawer-close">Đóng ✕</button></div>
      <a href="/">Trang chủ</a><a href="/products">Cửa hàng</a><a href="/products">Sản phẩm</a><a href="/cart">Giỏ hàng</a><a href="/">Liên hệ</a><a href="/account">Tài khoản</a>
      </div>
      <main id="main">
      <section className="auth-section">
      <div className="container">
      <div className="auth-shell">
      <aside className="auth-aside">
      <div>
      <span className="eyebrow">TÀI KHOẢN SPRYLO</span>
      <h2>Chào mừng bạn quay lại.</h2>
      <p>Đăng nhập để theo dõi đơn hàng, lưu địa chỉ giao hàng và thanh toán chỉ với vài cú nhấp.</p>
      </div>
      <ul className="perk-list">
      <li><span className="ic">📦</span><span><strong>Theo dõi đơn hàng</strong><span className="sub">Cập nhật trạng thái giao hàng theo thời gian thực</span></span></li>
      <li><span className="ic">📍</span><span><strong>Sổ địa chỉ</strong><span className="sub">Lưu tối đa 10 địa chỉ, chọn nhanh khi thanh toán</span></span></li>
      <li><span className="ic">🔒</span><span><strong>Bảo mật</strong><span className="sub">Mật khẩu được băm Argon2id, phiên đăng nhập có thời hạn</span></span></li>
      </ul>
      </aside>
      <div className="auth-main">
      <div className="crumbs"><a href="/">Trang chủ</a> <span className="sep">›</span> <span>Đăng nhập</span></div>
      <h1>Đăng nhập</h1>
      <p className="lead">Chưa có tài khoản? <a className="link" href="/register">Tạo tài khoản mới</a></p>
      <div id="form-alert" className="alert" hidden={true}></div>
      <form id="login-form" noValidate={true}>
      <div className="field">
      <label htmlFor="login-email">Email</label>
      <input id="login-email" name="email" type="email" inputMode="email" autoComplete="username" maxLength="254" required={true} placeholder="ban@example.com" />
      </div>
      <div className="field">
      <label htmlFor="login-password">Mật khẩu</label>
      <div className="pw-wrap">
      <input id="login-password" name="password" type="password" autoComplete="current-password" maxLength="128" required={true} placeholder="••••••••" />
      <button type="button" className="pw-toggle" data-target="login-password" aria-label="Hiện mật khẩu" aria-pressed="false">HIỆN</button>
      </div>
      </div>
      <div className="field-inline">
      <label className="check"><input type="checkbox" name="remember" /> Ghi nhớ đăng nhập</label>
      <a className="link" href="forgot-password.html">Quên mật khẩu?</a>
      </div>
      <button className="btn btn--indigo btn--block btn--submit" type="submit">Đăng nhập →</button>
      </form>
      <p className="secure-note">Không chia sẻ mật khẩu với bất kỳ ai. Sprylo không bao giờ hỏi mật khẩu qua email hay điện thoại.</p>
      </div>
      </div>
      </div>
      </section>
      </main>
      <footer className="site-footer">
      <div className="container">
      <div className="footer-grid">
      <div className="footer-brand">
      <div className="mark"><span className="brand-mark">S</span> Sprylo</div>
      <p>Đồ công nghệ chính hãng — giao nhanh, hỗ trợ tận tâm.</p>
      </div>
      <div className="footer-col"><h4>Danh mục nổi bật</h4><ul>
      <li><a href="/products">Laptop & PC</a></li><li><a href="/products">Nhà thông minh</a></li>
      <li><a href="/products">Đồng hồ thông minh</a></li><li><a href="/products">Tai nghe</a></li>
      </ul></div>
      <div className="footer-col"><h4>Hỗ trợ khách hàng</h4><ul>
      <li><a href="/account">Tài khoản của tôi</a></li><li><a href="#">Đơn hàng của tôi</a></li>
      <li><a href="#">Chính sách vận chuyển</a></li><li><a href="#">Trung tâm trợ giúp</a></li>
      </ul></div>
      <div className="footer-col"><h4>Tài nguyên</h4><ul>
      <li><a href="#">Blog</a></li><li><a href="#">Bản tin</a></li>
      <li><a href="#">Hỗ trợ</a></li><li><a href="#">Đánh giá</a></li>
      </ul></div>
      <div className="footer-col"><h4>Về chúng tôi</h4><ul>
      <li><a href="#">Giới thiệu</a></li><li><a href="/">Liên hệ</a></li>
      <li><a href="#">Điều khoản</a></li><li><a href="#">Quyền riêng tư</a></li>
      </ul></div>
      </div>
      <div className="footer-bottom">
      <span>© 2026 Sprylo · IT4409 — Nhóm 16</span>
      <span>Microservices · Spring Boot</span>
      </div>
      </div>
      </footer>
    </div>
  );
}
