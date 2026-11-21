package com.proyecto.ms_notificaciones.consumer;

import com.proyecto.ms_notificaciones.dto.AlertaDisparadaEvent;
import com.proyecto.ms_notificaciones.exception.PayloadInvalidoException;
import org.springframework.stereotype.Component;

/** Reglas mínimas del payload de una alerta disparada. */
@Component
public class AlertaDisparadaValidator {

    public void validar(AlertaDisparadaEvent e) {
        if (e == null) {
            throw new PayloadInvalidoException("Evento vacío");
        }
        if (e.alertaId() == null) {
            throw new PayloadInvalidoException("Falta alertaId");
        }
        if (e.usuarioId() == null || e.usuarioId().isBlank()) {
            throw new PayloadInvalidoException("Falta usuarioId");
        }
        if (e.simbolo() == null || e.simbolo().isBlank()) {
            throw new PayloadInvalidoException("Falta simbolo");
        }
        if (e.precioDisparo() == null || e.precioDisparo().signum() <= 0) {
            throw new PayloadInvalidoException("precioDisparo debe ser > 0: " + e.precioDisparo());
        }
        if (e.timestamp() == null) {
            throw new PayloadInvalidoException("Falta timestamp");
        }
    }
}
