package vn.it4409.g16.auth.web.dto;

import jakarta.validation.constraints.NotNull;
import vn.it4409.g16.auth.user.UserAccount;

/** "ACTIVE" hoặc "DISABLED". Giá trị khác -> Jackson báo lỗi -> 400. */
public record ChangeStatusRequest(@NotNull(message = "Chưa chọn trạng thái.") UserAccount.Status status) {}
