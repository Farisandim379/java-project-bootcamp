package com.bootcamp19.helloworld.exception;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException(String username) {
        super("User with name " + username + " already exist");
    }
}
