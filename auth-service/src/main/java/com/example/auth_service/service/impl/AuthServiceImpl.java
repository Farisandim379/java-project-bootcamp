package com.example.auth_service.service.impl;

import com.example.auth_service.dto.request.AuthRequestDto;
import com.example.auth_service.dto.response.AuthResponseDto;
import com.example.auth_service.entity.UserEntity;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.exception.UserAlreadyExistException;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.service.AuthService;
import com.example.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public String register(AuthRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistException(request.getUsername());
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return "User " + user.getUsername() + " berhasil didaftarkan";
    }

    @Override
    public AuthResponseDto login(AuthRequestDto request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Username atau password salah"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Username atau password salah");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponseDto(user.getUsername(), token);
    }
}
