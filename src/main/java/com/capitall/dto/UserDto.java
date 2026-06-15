package com.capitall.dto;

import com.capitall.model.UserRole;
import java.util.UUID;

public record UserDto(
    UUID id,
    String username,
    String email,
    UserRole role,
    boolean enabled
) {}
