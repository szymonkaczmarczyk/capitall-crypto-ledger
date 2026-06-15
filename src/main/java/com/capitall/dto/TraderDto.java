package com.capitall.dto;

import java.time.LocalDateTime;

public record TraderDto(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt
) {}
