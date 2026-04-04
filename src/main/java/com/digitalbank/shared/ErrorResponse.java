package com.digitalbank.shared;

public record ErrorResponse(int status, String title, String detail) {
}
