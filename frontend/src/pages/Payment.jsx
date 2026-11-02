import { useEffect } from "react";

export default function Payment() {
  useEffect(() => {
    document.title = "Thanh toán | Sprylo";
  }, []);

  return (
    <>
      <header className="topbar"><div className="brand">Hoàng Đức Vinh <span>| Thanh toán</span></div><div className="user">Xin chào, Hoàng Đức Vinh</div></header>
      <main className="container">
      <div className="heading"><div><p className="eyebrow">TRUNG TÂM QUẢN LÝ</p><h1>Thanh toán</h1><p className="muted">Theo dõi giao dịch, xác nhận thanh toán và hoàn tiền</p></div><button className="primary" type="button">+ Tạo mới</button></div>
      <div className="stats"><article><span>Tổng giao dịch</span><strong>128</strong></article><article><span>Đã thanh toán</span><strong className="green">96</strong></article><article><span>Đang chờ</span><strong className="orange">18</strong></article><article><span>Đã hoàn tiền</span><strong>14</strong></article></div>
      <section className="panel"><h2>Tra cứu giao dịch</h2><div className="filters"><label>Mã giao dịch<input placeholder="Nhập mã giao dịch" /></label><label>Mã đơn hàng<input placeholder="VD: DH001" /></label><label>Phương thức<select><option>Tất cả phương thức</option><option>COD</option><option>Ví điện tử</option><option>Thẻ ngân hàng</option></select></label><label>Trạng thái<select><option>Tất cả trạng thái</option><option>Thành công</option><option>Đang chờ</option><option>Đã hoàn tiền</option></select></label><button className="primary">Tìm kiếm</button></div></section>
      <section className="panel"><h2>Danh sách giao dịch</h2><div className="table-wrap"><table><thead><tr><th>Mã giao dịch</th><th>Mã đơn hàng</th><th>Khách hàng</th><th>Phương thức</th><th>Số tiền</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
      <tr><td>GD1001</td><td>DH001</td><td>Nguyễn Văn An</td><td>Ví điện tử</td><td className="price">542.000₫</td><td><span className="status success">Thành công</span></td><td><button className="link">Chi tiết</button></td></tr>
      <tr><td>GD1002</td><td>DH002</td><td>Trần Minh Anh</td><td>COD</td><td className="price">159.000₫</td><td><span className="status waiting">Đang chờ</span></td><td><button className="link">Xác nhận</button></td></tr>
      <tr><td>GD1003</td><td>DH003</td><td>Lê Thu Hà</td><td>Thẻ ngân hàng</td><td className="price">129.000₫</td><td><span className="status refunded">Đã hoàn tiền</span></td><td><button className="link">Chi tiết</button></td></tr>
      <tr><td>GD1004</td><td>DH004</td><td>Phạm Quang Huy</td><td>Ví điện tử</td><td className="price">320.000₫</td><td><span className="status success">Thành công</span></td><td><button className="link">Chi tiết</button></td></tr>
      </tbody></table></div></section>
      <footer>© 2026 Hoàng Đức Vinh · Hệ thống quản lý bán hàng</footer>
      </main>
    </>
  );
}
