package com.nikoladesnica.mastermind.domain.errors;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}