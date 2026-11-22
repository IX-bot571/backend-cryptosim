package com.proyecto.ms_alertas.exception;

/** Campos incoherentes con el tipo de alerta (400). */
public class AlertaInvalidaException extends RuntimeException {

    public AlertaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
