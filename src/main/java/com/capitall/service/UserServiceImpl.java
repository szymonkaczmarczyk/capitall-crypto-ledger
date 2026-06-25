package com.capitall.service;

import com.capitall.dto.CreateUserRequest;
import com.capitall.dto.UserDto;
import com.capitall.exception.EmailAlreadyExistsException;
import com.capitall.exception.ResourceNotFoundException;
import com.capitall.model.User;
import com.capitall.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email " + request.email() + " is already in use");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .role(request.role())
                .build();

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto registerUser(com.capitall.dto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email " + request.email() + " is already in use");
        }

        String activationToken = java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .phoneNumber(normalizePhone(request.phoneNumber()))
                .password(passwordEncoder.encode(request.password()))
                .role(com.capitall.model.UserRole.USER)
                .enabled(false)
                .build();
        user.setActivationToken(activationToken);
        user.setActivationTokenExpires(java.time.LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    @Override
    public UserDto getUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isEnabled()
        );
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
