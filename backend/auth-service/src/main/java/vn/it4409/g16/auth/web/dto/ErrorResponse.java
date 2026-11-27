package vn.it4409.g16.auth.web.dto;

import java.util.List;

public record ErrorResponse(String code, String message, List<String> details) {}
