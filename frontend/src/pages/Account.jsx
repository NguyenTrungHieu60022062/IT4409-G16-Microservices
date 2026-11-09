import { useEffect } from "react";
import "../styles/sprylo.css";
import "../styles/pages/auth.css";

export default function Account() {
  useEffect(() => {
    document.title = "Tài khoản | Sprylo";
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
      <a href="/account" className="icon-btn" aria-label="Tài khoản" aria-current="page"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="8" r="4"></circle><path d="M4 21c0-4 4-7 8-7s8 3 8 7"></path></svg></a>
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
      <section className="page-head">
      <div className="container">
      <div className="crumbs"><a href="/">Trang chủ</a> <span className="sep">›</span> <span>Tài khoản của tôi</span></div>
      <h1>Tài khoản của tôi</h1>
      <p>Quản lý thông tin cá nhân, địa chỉ giao hàng và cài đặt bảo mật.</p>
      </div>
      </section>
      <div className="container">
      <div id="page-alert" className="alert" hidden={true}></div>
      <p id="page-loading" className="loading">Đang tải thông tin tài khoản…</p>
      <div id="account-app" className="account-layout" hidden={true}>
      {/*  Sidebar  */}
      <aside className="account-nav" aria-label="Menu tài khoản">
      <div className="account-user">
      <div className="avatar" id="avatar" aria-hidden="true">?</div>
      <div>
      <div className="name" id="side-name"></div>
      <div className="email" id="side-email"></div>
      </div>
      </div>
      <div className="account-tabs" role="tablist" aria-orientation="vertical">
      <button type="button" role="tab" id="tab-profile" data-tab="profile" aria-controls="panel-profile"><span className="ic">👤</span>Hồ sơ cá nhân</button>
      <button type="button" role="tab" id="tab-addresses" data-tab="addresses" aria-controls="panel-addresses"><span className="ic">📍</span>Sổ địa chỉ</button>
      <button type="button" role="tab" id="tab-security" data-tab="security" aria-controls="panel-security"><span className="ic">🔒</span>Bảo mật</button>
      </div>
      <button type="button" className="logout" id="logout"><span className="ic">⎋</span>Đăng xuất</button>
      </aside>
      <div>
      {/*  Hồ sơ  */}
      <section className="panel" id="panel-profile" role="tabpanel" aria-labelledby="tab-profile">
      <div className="panel-head">
      <div>
      <h2>Hồ sơ cá nhân</h2>
      <p>Thông tin này dùng để liên hệ và tự điền khi bạn đặt hàng.</p>
      </div>
      </div>
      <div id="profile-alert" className="alert" hidden={true}></div>
      <form id="profile-form" noValidate={true}>
      <div className="field">
      <label htmlFor="pf-email">Email đăng nhập</label>
      <input id="pf-email" name="email" type="email" readOnly={true} />
      <span className="hint">Email dùng để đăng nhập và không thể thay đổi tại đây.</span>
      </div>
      <div className="field">
      <label htmlFor="pf-name">Họ và tên</label>
      <input id="pf-name" name="fullName" type="text" autoComplete="name" maxLength="100" placeholder="Nguyễn Văn A" />
      </div>
      <div className="field-row">
      <div className="field">
      <label htmlFor="pf-phone">Số điện thoại</label>
      <input id="pf-phone" name="phone" type="tel" inputMode="tel" autoComplete="tel" maxLength="12" placeholder="0912345678" />
      </div>
      <div className="field">
      <label htmlFor="pf-dob">Ngày sinh</label>
      <input id="pf-dob" name="dob" type="date" min="1900-01-01" autoComplete="bday" />
      </div>
      </div>
      <div className="form-actions">
      <button type="button" className="btn btn--ghost" id="profile-reset">Hủy thay đổi</button>
      <button type="submit" className="btn btn--indigo">Lưu thay đổi</button>
      </div>
      </form>
      </section>
      {/*  Sổ địa chỉ  */}
      <section className="panel" id="panel-addresses" role="tabpanel" aria-labelledby="tab-addresses" hidden={true}>
      <div className="panel-head">
      <div>
      <h2>Sổ địa chỉ</h2>
      <p id="address-count">0/10 địa chỉ</p>
      </div>
      <button type="button" className="btn btn--indigo" id="add-address">+ Thêm địa chỉ</button>
      </div>
      <div id="address-alert" className="alert" hidden={true}></div>
      <div className="address-grid" id="address-grid"></div>
      </section>
      {/*  Bảo mật  */}
      <section className="panel" id="panel-security" role="tabpanel" aria-labelledby="tab-security" hidden={true}>
      <div className="panel-head">
      <div>
      <h2>Bảo mật</h2>
      <p>Đổi mật khẩu định kỳ khi nghi ngờ bị lộ. Sau khi đổi, bạn sẽ bị đăng xuất khỏi mọi thiết bị.</p>
      </div>
      </div>
      <div id="security-alert" className="alert" hidden={true}></div>
      <h3 className="section-title">Đổi mật khẩu</h3>
      <form id="password-form" noValidate={true}>
      {/*  Trường username ẩn giúp trình quản lý mật khẩu biết đang đổi cho tài khoản nào  */}
      <input type="email" name="username" autoComplete="username" hidden={true} />
      <div className="field">
      <label htmlFor="sec-current">Mật khẩu hiện tại</label>
      <div className="pw-wrap">
      <input id="sec-current" name="currentPassword" type="password" autoComplete="current-password" maxLength="128" required={true} />
      <button type="button" className="pw-toggle" data-target="sec-current" aria-label="Hiện mật khẩu" aria-pressed="false">HIỆN</button>
      </div>
      </div>
      <div className="field-row">
      <div className="field">
      <label htmlFor="sec-new">Mật khẩu mới</label>
      <div className="pw-wrap">
      <input id="sec-new" name="newPassword" type="password" autoComplete="new-password" minLength="8" maxLength="128" required={true} />
      <button type="button" className="pw-toggle" data-target="sec-new" aria-label="Hiện mật khẩu" aria-pressed="false">HIỆN</button>
      </div>
      <div className="meter" id="np-meter" data-score="0" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
      <span className="hint" id="np-hint" aria-live="polite"></span>
      </div>
      <div className="field">
      <label htmlFor="sec-confirm">Nhập lại mật khẩu mới</label>
      <div className="pw-wrap">
      <input id="sec-confirm" name="confirmPassword" type="password" autoComplete="new-password" maxLength="128" required={true} />
      <button type="button" className="pw-toggle" data-target="sec-confirm" aria-label="Hiện mật khẩu" aria-pressed="false">HIỆN</button>
      </div>
      </div>
      </div>
      <div className="form-actions">
      <button type="submit" className="btn btn--indigo">Cập nhật mật khẩu</button>
      </div>
      </form>
      <div className="security-list">
      <div className="security-item">
      <div>
      <h3>Đăng xuất khỏi tất cả thiết bị</h3>
      <p>Thu hồi mọi phiên đăng nhập (máy tính, điện thoại…). Dùng khi bạn nghi ngờ tài khoản bị người khác sử dụng.</p>
      </div>
      <button type="button" className="btn btn--danger-ghost" id="logout-all">Đăng xuất tất cả</button>
      </div>
      </div>
      </section>
      </div>
      </div>
      </div>
      {/*  Hộp thoại thêm/sửa địa chỉ  */}
      <dialog className="modal" id="address-dialog" aria-labelledby="address-dialog-title">
      <form id="address-form" noValidate={true}>
      <h2 id="address-dialog-title">Thêm địa chỉ mới</h2>
      <div id="dialog-alert" className="alert" hidden={true}></div>
      <div className="field-row">
      <div className="field">
      <label htmlFor="ad-receiver">Người nhận</label>
      <input id="ad-receiver" name="receiver" type="text" autoComplete="shipping name" maxLength="100" required={true} />
      </div>
      <div className="field">
      <label htmlFor="ad-phone">Số điện thoại</label>
      <input id="ad-phone" name="phone" type="tel" inputMode="tel" autoComplete="shipping tel" maxLength="12" required={true} placeholder="0912345678" />
      </div>
      </div>
      <div className="field">
      <label htmlFor="ad-line1">Địa chỉ cụ thể</label>
      <input id="ad-line1" name="line1" type="text" autoComplete="shipping address-line1" maxLength="200" required={true} placeholder="Số nhà, tên đường" />
      </div>
      <div className="field-row">
      <div className="field">
      <label htmlFor="ad-ward">Phường / Xã</label>
      <input id="ad-ward" name="ward" type="text" maxLength="100" required={true} />
      </div>
      <div className="field">
      <label htmlFor="ad-district">Quận / Huyện</label>
      <input id="ad-district" name="district" type="text" autoComplete="shipping address-level2" maxLength="100" required={true} />
      </div>
      </div>
      <div className="field">
      <label htmlFor="ad-province">Tỉnh / Thành phố</label>
      <input id="ad-province" name="province" type="text" autoComplete="shipping address-level1" maxLength="100" required={true} />
      </div>
      <label className="check"><input type="checkbox" name="isDefault" /> Đặt làm địa chỉ mặc định</label>
      <div className="modal-actions">
      <button type="button" className="btn btn--ghost" id="address-cancel">Hủy</button>
      <button type="submit" className="btn btn--indigo">Lưu địa chỉ</button>
      </div>
      </form>
      </dialog>
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
