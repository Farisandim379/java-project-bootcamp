package com.example.auth_service.exception;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String username) {
        super("Username '" + username + "' sudah terdaftar");
    }
}
