package com.proyecto.ms_alertas.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento publicado en alertas.disparadas (clave = usuarioId). */
public record AlertaDisparadaEvent(Long alertaId, String usuarioId, String simbolo, String tipo,
                                   BigDecimal valorObjetivo, BigDecimal precioDisparo,
                                   Instant timestamp, String mensaje) {}
