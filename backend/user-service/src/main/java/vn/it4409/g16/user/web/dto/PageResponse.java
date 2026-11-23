package vn.it4409.g16.user.web.dto;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long total) {}
