import { useEffect } from "react";

export default function Notification() {
  useEffect(() => {
    document.title = "Thông báo | Sprylo";
  }, []);

  return (
    <>
      <header className="topbar"><div className="brand">Hoàng Đức Vinh <span>| Thông báo</span></div><div className="user">Xin chào, Hoàng Đức Vinh</div></header>
      <main className="container">
      <div className="heading"><div><p className="eyebrow">TRUNG TÂM QUẢN LÝ</p><h1>Thông báo</h1><p className="muted">Quản lý thông báo và email xác nhận đơn hàng</p></div><button className="primary" type="button">+ Tạo mới</button></div>
      <div className="stats"><article><span>Tổng thông báo</span><strong>256</strong></article><article><span>Đã gửi</span><strong className="green">231</strong></article><article><span>Đang chờ gửi</span><strong className="orange">15</strong></article><article><span>Gửi thất bại</span><strong className="red">10</strong></article></div>
      <section className="panel"><h2>Tạo thông báo mới</h2><div className="form"><label>Loại thông báo<select><option>Xác nhận đơn hàng</option><option>Thanh toán thành công</option><option>Cập nhật vận chuyển</option><option>Khuyến mãi</option></select></label><label>Người nhận<input type="email" placeholder="email@example.com" /></label><label className="full">Tiêu đề<input placeholder="Nhập tiêu đề thông báo" /></label><label className="full">Nội dung<textarea rows="3" placeholder="Nhập nội dung thông báo hoặc email"></textarea></label><div className="actions full"><button className="secondary">Làm mới</button><button className="primary">Gửi thông báo</button></div></div></section>
      <section className="panel"><h2>Lịch sử thông báo</h2>
      <div className="notice"><div className="notice-icon">✓</div><div className="notice-body"><b>Xác nhận đơn hàng DH001 <span className="status success">Đã gửi</span></b><p>Email xác nhận đã gửi đến nguyenvana@example.com</p><small>23/10/2026 · 09:35 · Mã TB001</small></div><button className="link">Chi tiết</button></div>
      <div className="notice"><div className="notice-icon blue">↗</div><div className="notice-body"><b>Cập nhật vận chuyển DH002 <span className="status success">Đã gửi</span></b><p>Thông báo đơn hàng đang được vận chuyển đến khách hàng</p><small>23/10/2026 · 09:10 · Mã TB002</small></div><button className="link">Chi tiết</button></div>
      <div className="notice"><div className="notice-icon green">₫</div><div className="notice-body"><b>Thanh toán thành công DH003 <span className="status waiting">Đang chờ</span></b><p>Thông báo xác nhận thanh toán đang chờ hệ thống xử lý</p><small>23/10/2026 · 08:55 · Mã TB003</small></div><button className="link">Chi tiết</button></div>
      <div className="notice"><div className="notice-icon red">!</div><div className="notice-body"><b>Email chưa gửi được DH004 <span className="status failed">Thất bại</span></b><p>Kiểm tra địa chỉ người nhận và thử gửi lại</p><small>22/10/2026 · 17:40 · Mã TB004</small></div><button className="link">Gửi lại</button></div></section>
      <footer>© 2026 Hoàng Đức Vinh · Hệ thống quản lý bán hàng</footer>
      </main>
    </>
  );
}
