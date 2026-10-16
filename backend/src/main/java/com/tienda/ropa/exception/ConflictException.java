package com.tienda.ropa.exception;

/** Conflicto de integridad o estado invalido para la operacion (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
