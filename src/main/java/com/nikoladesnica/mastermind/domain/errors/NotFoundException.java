package com.nikoladesnica.mastermind.domain.errors;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
