package com.skillstorm.auth_service.Exceptions;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account already exists for email: " + email);
    }
}