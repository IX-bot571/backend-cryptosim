package com.proyecto.ms_admin_kafka.exception;

public class KafkaNoDisponibleException extends RuntimeException {
    public KafkaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
