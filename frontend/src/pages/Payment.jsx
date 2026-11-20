import { useEffect } from "react";
import "../styles/sprylo.css";
import "../styles/auth.css";

export default function Payment() {
  useEffect(() => {
    document.title = "Thanh toán | Sprylo";
  }, []);

  return (
    <div className="sprylo">
      <a className="skip-link" href="#main">Bỏ qua tới nội dung</a>

      <div className="utility">
        <div className="container">
          <span className="promo">
            <span className="tag">SALE</span>
            Miễn phí vận chuyển cho đơn từ 500.000₫ · Đổi trả trong 30 ngày
          </span>
          <span className="links">
            <a href="#">Tra cứu đơn hàng</a>
            <a href="#">Trợ giúp</a>
            <a href="#">VI · VND</a>
          </span>
        </div>
      </div>

      <header className="site-header">
        <div className="container">
          <a href="/" className="brand">
            <span className="brand-mark">S</span> Sprylo
          </a>

          <form className="search" role="search" action="shop.html">
            <input
              type="search"
              name="q"
              maxLength="100"
              placeholder="Tìm sản phẩm, thương hiệu, danh mục…"
              aria-label="Tìm kiếm sản phẩm"
            />
            <button type="submit" aria-label="Tìm kiếm">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
                <circle cx="11" cy="11" r="7"></circle>
                <path d="M21 21l-4.3-4.3"></path>
              </svg>
            </button>
          </form>

          <div className="icon-row">
            <a href="/account" className="icon-btn" aria-label="Tài khoản">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <circle cx="12" cy="8" r="4"></circle>
                <path d="M4 21c0-4 4-7 8-7s8 3 8 7"></path>
              </svg>
            </a>

            <a href="#" className="icon-btn" aria-label="Yêu thích">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
              </svg>
            </a>

            <a href="/cart" className="icon-btn icon-btn--cart" aria-label="Giỏ hàng">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 2l-2 5v13a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7l-2-5z"></path>
                <path d="M4 7h16"></path>
                <path d="M16 11a4 4 0 0 1-8 0"></path>
              </svg>
            </a>

            <button className="nav-toggle" aria-label="Mở menu" aria-expanded="false">≡</button>
          </div>
        </div>
      </header>

      <nav className="nav-bar" aria-label="Điều hướng chính">
        <div className="container">
          <a href="/products" className="all-cats">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
              <line x1="3" y1="6" x2="21" y2="6"></line>
              <line x1="3" y1="12" x2="21" y2="12"></line>
              <line x1="3" y1="18" x2="21" y2="18"></line>
            </svg>
            Tất cả danh mục
          </a>

          <div className="main-nav">
            <a href="/">Trang chủ</a>
            <a href="/products">Cửa hàng</a>
            <a href="/products">Sản phẩm</a>
            <a href="/cart">Giỏ hàng</a>
            <a href="/">Liên hệ</a>
          </div>

          <span className="nav-cta">GIẢM ĐẾN <strong>60%</strong> MỌI SẢN PHẨM</span>
        </div>
      </nav>

      <div className="drawer" id="drawer" aria-hidden="true">
        <div className="drawer-head">
          <a href="/" className="brand">
            <span className="brand-mark">S</span> Sprylo
          </a>
          <button className="drawer-close">Đóng ✕</button>
        </div>
        <a href="/">Trang chủ</a>
        <a href="/products">Cửa hàng</a>
        <a href="/products">Sản phẩm</a>
        <a href="/cart">Giỏ hàng</a>
        <a href="/">Liên hệ</a>
        <a href="/account">Tài khoản</a>
        <a href="/notifications">Thông báo</a>
        <a href="/orders">Đơn hàng</a>
        <a href="/payments">Thanh toán</a>
        <a href="/shipping">Vận chuyển</a>
      </div>

      <main id="main">
        <section className="page-head">
          <div className="container">
            <div className="crumbs">
              <a href="/">Trang chủ</a>
              <span className="sep">›</span>
              <span>Thanh toán</span>
            </div>
            <h1>Thanh toán</h1>
            <p>Theo dõi giao dịch, xác nhận thanh toán và hoàn tiền.</p>
          </div>
        </section>

        <div className="container">
          <div className="heading">
            <div>
              <p className="eyebrow">TRUNG TÂM QUẢN LÝ</p>
              <h2>Quản lý giao dịch</h2>
              <p className="muted">Theo dõi và quản lý các giao dịch thanh toán.</p>
            </div>
            <button className="btn btn--indigo" type="button">+ Tạo mới</button>
          </div>

          <div className="stats">
            <article>
              <span>Tổng giao dịch</span>
              <strong>128</strong>
            </article>
            <article>
              <span>Đã thanh toán</span>
              <strong className="green">96</strong>
            </article>
            <article>
              <span>Đang chờ</span>
              <strong className="orange">18</strong>
            </article>
            <article>
              <span>Đã hoàn tiền</span>
              <strong>14</strong>
            </article>
          </div>

          <section className="panel">
            <div className="panel-head">
              <div>
                <h2>Tra cứu giao dịch</h2>
                <p>Tìm kiếm giao dịch theo mã đơn hàng và trạng thái.</p>
              </div>
            </div>

            <div className="field-row">
              <div className="field">
                <label>Mã giao dịch</label>
                <input placeholder="Nhập mã giao dịch" />
              </div>
              <div className="field">
                <label>Mã đơn hàng</label>
                <input placeholder="VD: DH001" />
              </div>
            </div>

            <div className="field-row">
              <div className="field">
                <label>Phương thức</label>
                <select defaultValue="all">
                  <option value="all">Tất cả phương thức</option>
                  <option>COD</option>
                  <option>Ví điện tử</option>
                  <option>Thẻ ngân hàng</option>
                </select>
              </div>
              <div className="field">
                <label>Trạng thái</label>
                <select defaultValue="all">
                  <option value="all">Tất cả trạng thái</option>
                  <option>Thành công</option>
                  <option>Đang chờ</option>
                  <option>Đã hoàn tiền</option>
                </select>
              </div>
            </div>

            <div className="form-actions">
              <button className="btn btn--indigo" type="button">Tìm kiếm</button>
            </div>
          </section>

          <section className="panel">
            <div className="panel-head">
              <div>
                <h2>Danh sách giao dịch</h2>
                <p>Thông tin các giao dịch thanh toán trong hệ thống.</p>
              </div>
            </div>

            <div style={{ overflowX: "auto" }}>
              <table>
                <thead>
                  <tr>
                    <th>Mã giao dịch</th>
                    <th>Mã đơn hàng</th>
                    <th>Khách hàng</th>
                    <th>Phương thức</th>
                    <th>Số tiền</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>GD1001</td>
                    <td>DH001</td>
                    <td>Nguyễn Văn An</td>
                    <td>Ví điện tử</td>
                    <td className="price">542.000₫</td>
                    <td><span className="status success">Thành công</span></td>
                    <td><button className="btn btn--ghost" type="button">Chi tiết</button></td>
                  </tr>
                  <tr>
                    <td>GD1002</td>
                    <td>DH002</td>
                    <td>Trần Minh Anh</td>
                    <td>COD</td>
                    <td className="price">159.000₫</td>
                    <td><span className="status waiting">Đang chờ</span></td>
                    <td><button className="btn btn--ghost" type="button">Xác nhận</button></td>
                  </tr>
                  <tr>
                    <td>GD1003</td>
                    <td>DH003</td>
                    <td>Lê Thu Hà</td>
                    <td>Thẻ ngân hàng</td>
                    <td className="price">129.000₫</td>
                    <td><span className="status refunded">Đã hoàn tiền</span></td>
                    <td><button className="btn btn--ghost" type="button">Chi tiết</button></td>
                  </tr>
                  <tr>
                    <td>GD1004</td>
                    <td>DH004</td>
                    <td>Phạm Quang Huy</td>
                    <td>Ví điện tử</td>
                    <td className="price">320.000₫</td>
                    <td><span className="status success">Thành công</span></td>
                    <td><button className="btn btn--ghost" type="button">Chi tiết</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </main>

      <footer className="site-footer">
        <div className="container">
          <div className="footer-grid">
            <div className="footer-brand">
              <div className="mark">
                <span className="brand-mark">S</span> Sprylo
              </div>
              <p>Đồ công nghệ chính hãng — giao nhanh, hỗ trợ tận tâm.</p>
            </div>

            <div className="footer-col">
              <h4>Danh mục nổi bật</h4>
              <ul>
                <li><a href="/products">Laptop & PC</a></li>
                <li><a href="/products">Nhà thông minh</a></li>
                <li><a href="/products">Đồng hồ thông minh</a></li>
                <li><a href="/products">Tai nghe</a></li>
              </ul>
            </div>

            <div className="footer-col">
              <h4>Hỗ trợ khách hàng</h4>
              <ul>
                <li><a href="/account">Tài khoản của tôi</a></li>
                <li><a href="/orders">Đơn hàng của tôi</a></li>
                <li><a href="/shipping">Chính sách vận chuyển</a></li>
                <li><a href="#">Trung tâm trợ giúp</a></li>
              </ul>
            </div>

            <div className="footer-col">
              <h4>Tài nguyên</h4>
              <ul>
                <li><a href="#">Blog</a></li>
                <li><a href="#">Bản tin</a></li>
                <li><a href="#">Hỗ trợ</a></li>
                <li><a href="#">Đánh giá</a></li>
              </ul>
            </div>

            <div className="footer-col">
              <h4>Về chúng tôi</h4>
              <ul>
                <li><a href="#">Giới thiệu</a></li>
                <li><a href="/">Liên hệ</a></li>
                <li><a href="#">Điều khoản</a></li>
                <li><a href="#">Quyền riêng tư</a></li>
              </ul>
            </div>
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