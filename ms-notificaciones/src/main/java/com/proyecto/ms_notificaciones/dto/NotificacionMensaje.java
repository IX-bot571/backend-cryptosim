package com.proyecto.ms_notificaciones.dto;

/**
 * Mensaje de notificacion que ms-alertas publica en tareas.exchange.
 * Debe coincidir con el contrato (CONTRATO.md).
 */
public record NotificacionMensaje(
        Long referenciaId,
        String usuario,
        String mensaje) {
}
