package com.bootcamp19.helloworld.service.impl;

import com.bootcamp19.helloworld.dto.request.AuthRequestDto;
import com.bootcamp19.helloworld.dto.response.AuthResponseDto;
import com.bootcamp19.helloworld.entity.UserEntity;
import com.bootcamp19.helloworld.exception.UnauthorizedException;
import com.bootcamp19.helloworld.exception.UserAlreadyExistException;
import com.bootcamp19.helloworld.repository.UserRepository;
import com.bootcamp19.helloworld.service.AuthService;
import com.bootcamp19.helloworld.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    @Override
    public String register(AuthRequestDto authRequestDto) {
        if(userRepository.existsByUsername(authRequestDto.getUsername())){
            throw new UserAlreadyExistException(authRequestDto.getUsername());
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(authRequestDto.getUsername());
        userEntity.setPassword(passwordEncoder.encode(authRequestDto.getPassword()));
        userRepository.save(userEntity);
        return "User " + userEntity.getUsername() + " has been registered succesfully";
    }

    @Override
    public AuthResponseDto login(AuthRequestDto authRequestDto) {
        UserEntity userEntity = userRepository.findByUsername(authRequestDto.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Username atau password salah"));

        if(!passwordEncoder.matches(authRequestDto.getPassword(), userEntity.getPassword())){
            throw new UnauthorizedException("Username atau password salah");
        }
        String token = jwtUtil.generatedToken(userEntity.getUsername());

        return new AuthResponseDto(userEntity.getUsername(), token);
    }
}
