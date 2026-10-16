package com.tienda.ropa.exception;

/** Inventario insuficiente para completar la operacion (HTTP 409). */
public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) { super(message); }
}
