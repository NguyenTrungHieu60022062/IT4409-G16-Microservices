import { useEffect } from "react";

export default function ProductInventory() {
  useEffect(() => {
    document.title = "Sản phẩm & tồn kho | Sprylo";
  }, []);

  return (
    <>
      <section id="product-service">
      <h2>Product & Inventory Service</h2>
      <p>Quản lý sản phẩm, danh mục, giá bán và tồn kho.</p>
      <h3>Thêm sản phẩm</h3>
      <form>
      <label htmlFor="product-name">Tên sản phẩm:</label>
      <input type="text" id="product-name" required={true} />
      <label htmlFor="category">Danh mục:</label>
      <input type="text" id="category" />
      <label htmlFor="price">Giá bán:</label>
      <input type="number" id="price" min="0" />
      <label htmlFor="stock">Số lượng tồn kho:</label>
      <input type="number" id="stock" min="0" />
      <button type="button">Thêm sản phẩm</button>
      </form>
      <h3>Danh sách sản phẩm</h3>
      <table border="1">
      <thead>
      <tr>
      <th>Mã SP</th>
      <th>Tên sản phẩm</th>
      <th>Danh mục</th>
      <th>Giá</th>
      <th>Tồn kho</th>
      <th>Thao tác</th>
      </tr>
      </thead>
      <tbody>
      <tr>
      <td>1</td>
      <td>Bàn phím</td>
      <td>Phụ kiện</td>
      <td>500000</td>
      <td>20</td>
      <td>
      <button type="button">Sửa</button>
      <button type="button">Xóa</button>
      </td>
      </tr>
      </tbody>
      </table>
      </section>
    </>
  );
}
