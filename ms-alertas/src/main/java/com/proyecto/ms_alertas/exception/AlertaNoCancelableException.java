package com.proyecto.ms_alertas.exception;

import com.proyecto.ms_alertas.entity.EstadoAlerta;

public class AlertaNoCancelableException extends RuntimeException {
    public AlertaNoCancelableException(Long id, EstadoAlerta estado) {
        super("La alerta " + id + " no se puede cancelar: está " + estado);
    }
}
