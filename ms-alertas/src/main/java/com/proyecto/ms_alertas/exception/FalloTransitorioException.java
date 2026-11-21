package com.proyecto.ms_alertas.exception;

/** Fallo temporal (por ejemplo, BD no disponible). Es reintentable: el error handler aplica backoff. */
public class FalloTransitorioException extends RuntimeException {
    public FalloTransitorioException(String mensaje) {
        super(mensaje);
    }
}
