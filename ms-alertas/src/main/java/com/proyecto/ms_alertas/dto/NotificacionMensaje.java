package com.proyecto.ms_alertas.dto;

/** Mensaje publicado en tareas.exchange. Debe coincidir con CONTRATO.md y con ms-notificaciones. */
public record NotificacionMensaje(Long referenciaId, String usuario, String mensaje) {
}
