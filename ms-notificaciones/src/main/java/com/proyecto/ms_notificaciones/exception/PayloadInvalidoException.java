package com.proyecto.ms_notificaciones.exception;

/** Evento Kafka con datos inválidos. No es reintentable: va directo al DLT. */
public class PayloadInvalidoException extends RuntimeException {
    public PayloadInvalidoException(String mensaje) {
        super(mensaje);
    }
}
