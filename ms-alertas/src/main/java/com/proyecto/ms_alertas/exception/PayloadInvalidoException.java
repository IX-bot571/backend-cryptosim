package com.proyecto.ms_alertas.exception;

/** Evento Kafka con datos inválidos. No es reintentable: reintentar no lo arregla. */
public class PayloadInvalidoException extends RuntimeException {
    public PayloadInvalidoException(String mensaje) {
        super(mensaje);
    }
}
