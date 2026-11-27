package vn.it4409.g16.auth.web.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, boolean current, Instant startedAt, Instant lastActiveAt,
                              Instant expiresAt, String ip, String userAgent, boolean rememberMe) {}
