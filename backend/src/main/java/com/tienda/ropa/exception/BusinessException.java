package com.tienda.ropa.exception;

/** Excepcion base para reglas de negocio incumplidas (HTTP 422). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
