package com.proyecto.ms_alertas.exception;

public class AlertaNoEncontradaException extends RuntimeException {
    public AlertaNoEncontradaException(Long id) {
        super("Alerta no encontrada: " + id);
    }
}
