package com.proyecto.ms_precios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SimboloNoEncontradoException.class)
    ResponseEntity<Map<String, Object>> noEncontrado(SimboloNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<Map<String, Object>> validacion(HandlerMethodValidationException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Parámetros inválidos",
                "detalle", e.getAllErrors().stream().map(err -> String.valueOf(err.getDefaultMessage())).toList()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> tipo(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Parámetro inválido: " + e.getName()));
    }
}
