package com.proyecto.ms_precios.exception;

public class SimboloNoEncontradoException extends RuntimeException {
    public SimboloNoEncontradoException(String simbolo) {
        super("Símbolo no está en el catálogo: " + simbolo);
    }
}
