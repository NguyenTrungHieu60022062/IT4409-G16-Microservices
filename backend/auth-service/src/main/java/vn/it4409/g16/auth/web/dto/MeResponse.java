package vn.it4409.g16.auth.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Chỉ trả trường cần thiết — tuyệt đối không có passwordHash. Đừng bao giờ trả thẳng Entity ra JSON. */
public record MeResponse(UUID id, String email, String role, List<String> permissions,
                         Instant createdAt, Instant passwordChangedAt) {}
