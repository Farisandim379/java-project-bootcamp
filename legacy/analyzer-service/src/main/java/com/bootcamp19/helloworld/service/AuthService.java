package com.bootcamp19.helloworld.service;


import com.bootcamp19.helloworld.dto.request.AuthRequestDto;
import com.bootcamp19.helloworld.dto.response.AuthResponseDto;

public interface AuthService {
    String register(AuthRequestDto authRequestDto);
    AuthResponseDto login(AuthRequestDto authRequestDto);
}
