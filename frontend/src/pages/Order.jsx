import { useEffect } from "react";

export default function Order() {
  useEffect(() => {
    document.title = "Đơn hàng | Sprylo";
  }, []);

  return (
    <>
      <section id="order-service">
      <h2>Order Service</h2>
      <p>Tạo, theo dõi và hủy đơn hàng.</p>
      <h3>Thông tin đặt hàng</h3>
      <form>
      <label htmlFor="customer-name">Họ và tên:</label>
      <input type="text" id="customer-name" required={true} />
      <label htmlFor="address">Địa chỉ giao hàng:</label>
      <input type="text" id="address" required={true} />
      <label htmlFor="phone">Số điện thoại:</label>
      <input type="tel" id="phone" required={true} />
      <button type="button">Tạo đơn hàng</button>
      </form>
      <h3>Danh sách đơn hàng</h3>
      <table border="1">
      <thead>
      <tr>
      <th>Mã đơn</th>
      <th>Khách hàng</th>
      <th>Tổng tiền</th>
      <th>Trạng thái</th>
      <th>Thao tác</th>
      </tr>
      </thead>
      <tbody>
      <tr>
      <td>ORD001</td>
      <td>Nguyễn Văn A</td>
      <td>500000</td>
      <td>Đang xử lý</td>
      <td>
      <button type="button">Chi tiết</button>
      <button type="button">Hủy đơn</button>
      </td>
      </tr>
      </tbody>
      </table>
      </section>
    </>
  );
}
