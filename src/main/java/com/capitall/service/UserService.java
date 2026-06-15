package com.capitall.service;

import com.capitall.dto.CreateUserRequest;
import com.capitall.dto.UserDto;
import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDto createUser(CreateUserRequest request);
    UserDto registerUser(com.capitall.dto.RegisterRequest request);
    UserDto getUserById(UUID id);
    List<UserDto> getAllUsers();
    void deleteUser(UUID id);
}
