package com.tienda.ropa.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Manejo global de excepciones: respuesta uniforme, sin trazas internas. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(int status, String error, String message, List<FieldIssue> campos, OffsetDateTime timestamp) { }
    public record FieldIssue(String campo, String mensaje) { }

    private ResponseEntity<Object> build(HttpStatus status, String message, List<FieldIssue> campos) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message,
                campos == null ? List.of() : campos, OffsetDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> onValidation(MethodArgumentNotValidException ex) {
        List<FieldIssue> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldIssue(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene datos invalidos", campos);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> onTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Parametro '" + ex.getName() + "' con formato invalido", List.of());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> onBusiness(BusinessException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Object> onStock(InsufficientStockException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> onConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> onNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> onIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "La operacion viola restricciones de datos (valores duplicados o referencias existentes)", List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> onAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para ejecutar esta operacion", List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> onAuth(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales invalidas", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> onUnexpected(Exception ex, HttpServletRequest request) {
        // Id de correlacion para diagnostico sin exponer detalles al cliente
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("ruta", request.getRequestURI());
        log.error("Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno. Intente nuevamente.", List.of());
    }
}
