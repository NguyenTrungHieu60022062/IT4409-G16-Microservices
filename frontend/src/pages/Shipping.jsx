import { useEffect } from "react";
import "../styles/sprylo.css";
import "../styles/auth.css";

export default function Shipping() {
  useEffect(() => {
    document.title = "Vận chuyển | Sprylo";
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

            <button className="nav-toggle" aria-label="Mở menu" aria-expanded="false">
              ≡
            </button>
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

          <span className="nav-cta">
            GIẢM ĐẾN <strong>60%</strong> MỌI SẢN PHẨM
          </span>
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
      </div>

      <main id="main">
        <section className="page-head">
          <div className="container">
            <div className="crumbs">
              <a href="/">Trang chủ</a>
              <span className="sep">›</span>
              <span>Vận chuyển</span>
            </div>
            <h1>Quản lý vận chuyển</h1>
            <p>Theo dõi đơn giao hàng và cập nhật trạng thái vận chuyển</p>
          </div>
        </section>

        <div className="container">
          <section className="shipping-section">
            <div className="heading">
              <div>
                <p className="eyebrow">TRUNG TÂM QUẢN LÝ</p>
                <h2>Vận chuyển</h2>
                <p className="muted">
                  Theo dõi và quản lý toàn bộ vận đơn của cửa hàng
                </p>
              </div>
              <button className="btn btn--indigo" type="button">
                + Tạo mới
              </button>
            </div>

            <div className="stats">
              <article className="panel">
                <span>Chờ lấy hàng</span>
                <h2>12</h2>
              </article>
              <article className="panel">
                <span>Đang vận chuyển</span>
                <h2 className="blue">24</h2>
              </article>
              <article className="panel">
                <span>Giao thành công</span>
                <h2 className="green">86</h2>
              </article>
              <article className="panel">
                <span>Giao thất bại</span>
                <h2 className="red">3</h2>
              </article>
            </div>

            <section className="panel">
              <div className="panel-head">
                <h2>Tra cứu vận đơn</h2>
              </div>

              <div className="field-row">
                <div className="field">
                  <label htmlFor="tracking-code">Mã vận đơn</label>
                  <input id="tracking-code" placeholder="Nhập mã vận đơn" />
                </div>

                <div className="field">
                  <label htmlFor="order-code">Mã đơn hàng</label>
                  <input id="order-code" placeholder="VD: DH001" />
                </div>

                <div className="field">
                  <label htmlFor="shipping-company">Đơn vị vận chuyển</label>
                  <select id="shipping-company" defaultValue="">
                    <option value="">Tất cả đơn vị</option>
                    <option>Đơn vị A</option>
                    <option>Đơn vị B</option>
                  </select>
                </div>

                <div className="field">
                  <label htmlFor="shipping-status">Trạng thái</label>
                  <select id="shipping-status" defaultValue="">
                    <option value="">Tất cả trạng thái</option>
                    <option>Chờ lấy hàng</option>
                    <option>Đang vận chuyển</option>
                    <option>Giao thành công</option>
                    <option>Giao thất bại</option>
                  </select>
                </div>
              </div>

              <div className="form-actions">
                <button className="btn btn--indigo" type="button">
                  Tìm kiếm
                </button>
              </div>
            </section>

            <section className="panel">
              <div className="panel-head">
                <div>
                  <h2>Danh sách vận đơn</h2>
                  <p>Thông tin và trạng thái các đơn giao hàng.</p>
                </div>
              </div>

              <div style={{ overflowX: "auto" }}>
                <table>
                  <thead>
                    <tr>
                      <th>Mã vận đơn</th>
                      <th>Mã đơn hàng</th>
                      <th>Người nhận</th>
                      <th>Địa chỉ</th>
                      <th>Đơn vị vận chuyển</th>
                      <th>Trạng thái</th>
                      <th>Cập nhật</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>VC0001</td>
                      <td>DH001</td>
                      <td>Nguyễn Văn An</td>
                      <td>Cầu Giấy, Hà Nội</td>
                      <td>Đơn vị A</td>
                      <td>
                        <span className="status waiting">Chờ lấy hàng</span>
                      </td>
                      <td>
                        <button className="link" type="button">Cập nhật</button>
                      </td>
                    </tr>

                    <tr>
                      <td>VC0002</td>
                      <td>DH002</td>
                      <td>Trần Minh Anh</td>
                      <td>Đống Đa, Hà Nội</td>
                      <td>Đơn vị B</td>
                      <td>
                        <span className="status moving">Đang vận chuyển</span>
                      </td>
                      <td>
                        <button className="link" type="button">Cập nhật</button>
                      </td>
                    </tr>

                    <tr>
                      <td>VC0003</td>
                      <td>DH003</td>
                      <td>Lê Thu Hà</td>
                      <td>Nam Từ Liêm, Hà Nội</td>
                      <td>Đơn vị A</td>
                      <td>
                        <span className="status success">Giao thành công</span>
                      </td>
                      <td>
                        <button className="link" type="button">Chi tiết</button>
                      </td>
                    </tr>

                    <tr>
                      <td>VC0004</td>
                      <td>DH004</td>
                      <td>Phạm Quang Huy</td>
                      <td>Thanh Xuân, Hà Nội</td>
                      <td>Đơn vị B</td>
                      <td>
                        <span className="status failed">Giao thất bại</span>
                      </td>
                      <td>
                        <button className="link" type="button">Xử lý</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section className="panel">
              <div className="panel-head">
                <h2>Hành trình vận chuyển mẫu - VC0002</h2>
              </div>

              <div className="shipping-timeline">
                <div className="timeline-item completed">
                  <span className="timeline-dot">✓</span>
                  <div>
                    <strong>Đã xác nhận đơn hàng</strong>
                    <small>08:30 · 22/10/2026</small>
                  </div>
                </div>

                <div className="timeline-item completed">
                  <span className="timeline-dot">✓</span>
                  <div>
                    <strong>Đơn hàng đã được lấy</strong>
                    <small>11:15 · 22/10/2026</small>
                  </div>
                </div>

                <div className="timeline-item active">
                  <span className="timeline-dot">•</span>
                  <div>
                    <strong>Đang vận chuyển đến kho phân loại</strong>
                    <small>09:10 · 23/10/2026</small>
                  </div>
                </div>

                <div className="timeline-item">
                  <span className="timeline-dot">•</span>
                  <div>
                    <strong>Đang giao đến người nhận</strong>
                    <small>Đang chờ cập nhật</small>
                  </div>
                </div>
              </div>
            </section>
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
                <li><a href="#">Đơn hàng của tôi</a></li>
                <li><a href="#">Chính sách vận chuyển</a></li>
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