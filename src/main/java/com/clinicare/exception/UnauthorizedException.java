package com.clinicare.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
