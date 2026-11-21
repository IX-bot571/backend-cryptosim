package com.proyecto.ms_alertas.exception;

public class SimboloNoValidoException extends RuntimeException {
    public SimboloNoValidoException(String simbolo) {
        super("Símbolo no está en el catálogo: " + simbolo);
    }
}
