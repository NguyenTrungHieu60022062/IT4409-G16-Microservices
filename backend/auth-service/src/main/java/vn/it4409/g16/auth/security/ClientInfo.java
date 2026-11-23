package vn.it4409.g16.auth.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Thông tin thiết bị gửi request, dùng cho rate limit, audit log và danh sách phiên.
 *
 * <p>IP lấy từ getRemoteAddr(), KHÔNG đọc thẳng header X-Forwarded-For: header đó client tự đặt được,
 * kẻ tấn công đổi giá trị mỗi request là "lách" được rate limit theo IP. Khi chạy sau Gateway, bật
 * server.forward-headers-strategy=native và chỉ tin header do Gateway (proxy tin cậy) thêm vào.
 */
public record ClientInfo(String ip, String userAgent) {

    public static ClientInfo from(HttpServletRequest req) {
        return new ClientInfo(clean(req.getRemoteAddr(), 45), clean(req.getHeader("User-Agent"), 255));
    }

    /**
     * Bỏ ký tự điều khiển (CR, LF, tab...) — chống "log injection": User-Agent chứa "\n" có thể giả
     * mạo thêm một dòng log như "LOGIN_SUCCESS admin" để đánh lừa người đọc log.
     */
    static String clean(String value, int max) {
        if (value == null) return null;
        String s = value.replaceAll("\\p{Cntrl}", "");
        return s.length() > max ? s.substring(0, max) : s;
    }
}
