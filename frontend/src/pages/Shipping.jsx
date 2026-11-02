import { useEffect } from "react";

export default function Shipping() {
  useEffect(() => {
    document.title = "Vận chuyển | Sprylo";
  }, []);

  return (
    <>
      <header className="topbar"><div className="brand">Hoàng Đức Vinh <span>| Vận chuyển</span></div><div className="user">Xin chào, Hoàng Đức Vinh</div></header>
      <main className="container">
      <div className="heading"><div><p className="eyebrow">TRUNG TÂM QUẢN LÝ</p><h1>Vận chuyển</h1><p className="muted">Theo dõi đơn giao hàng và cập nhật trạng thái vận chuyển</p></div><button className="primary" type="button">+ Tạo mới</button></div>
      <div className="stats"><article><span>Chờ lấy hàng</span><strong>12</strong></article><article><span>Đang vận chuyển</span><strong className="blue">24</strong></article><article><span>Giao thành công</span><strong className="green">86</strong></article><article><span>Giao thất bại</span><strong className="red">3</strong></article></div>
      <section className="panel"><h2>Tra cứu vận đơn</h2><div className="filters"><label>Mã vận đơn<input placeholder="Nhập mã vận đơn" /></label><label>Mã đơn hàng<input placeholder="VD: DH001" /></label><label>Đơn vị vận chuyển<select><option>Tất cả đơn vị</option><option>Đơn vị A</option><option>Đơn vị B</option></select></label><label>Trạng thái<select><option>Tất cả trạng thái</option><option>Chờ lấy hàng</option><option>Đang giao</option><option>Đã giao</option></select></label><button className="primary">Tìm kiếm</button></div></section>
      <section className="panel"><h2>Danh sách vận đơn</h2><div className="table-wrap"><table><thead><tr><th>Mã vận đơn</th><th>Mã đơn hàng</th><th>Người nhận</th><th>Địa chỉ</th><th>Đơn vị vận chuyển</th><th>Trạng thái</th><th>Cập nhật</th></tr></thead><tbody>
      <tr><td>VC0001</td><td>DH001</td><td>Nguyễn Văn An</td><td>Cầu Giấy, Hà Nội</td><td>Đơn vị A</td><td><span className="status waiting">Chờ lấy hàng</span></td><td><button className="link">Cập nhật</button></td></tr>
      <tr><td>VC0002</td><td>DH002</td><td>Trần Minh Anh</td><td>Đống Đa, Hà Nội</td><td>Đơn vị B</td><td><span className="status moving">Đang vận chuyển</span></td><td><button className="link">Cập nhật</button></td></tr>
      <tr><td>VC0003</td><td>DH003</td><td>Lê Thu Hà</td><td>Nam Từ Liêm, Hà Nội</td><td>Đơn vị A</td><td><span className="status success">Giao thành công</span></td><td><button className="link">Chi tiết</button></td></tr>
      <tr><td>VC0004</td><td>DH004</td><td>Phạm Quang Huy</td><td>Thanh Xuân, Hà Nội</td><td>Đơn vị B</td><td><span className="status failed">Giao thất bại</span></td><td><button className="link">Xử lý</button></td></tr>
      </tbody></table></div></section>
      <section className="panel"><h2>Hành trình vận chuyển mẫu - VC0002</h2><div className="timeline"><p>🟢 <b>Đã xác nhận đơn hàng</b><small>08:30 · 22/10/2026</small></p><p>🟢 <b>Đơn hàng đã được lấy</b><small>11:15 · 22/10/2026</small></p><p>🟠 <b>Đang vận chuyển đến kho phân loại</b><small>09:10 · 23/10/2026</small></p><p>⚪ <b>Đang giao đến người nhận</b><small>Đang chờ cập nhật</small></p></div></section>
      <footer>© 2026 Hoàng Đức Vinh · Hệ thống quản lý bán hàng</footer>
      </main>
    </>
  );
}
