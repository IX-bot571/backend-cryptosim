package com.proyecto.ms_notificaciones.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento consumido de alertas.disparadas (lo publica ms-alertas; mismo JSON, ver CONTRATO.md). */
public record AlertaDisparadaEvent(Long alertaId, String usuarioId, String simbolo, String tipo,
                                   BigDecimal valorObjetivo, BigDecimal precioDisparo,
                                   Instant timestamp, String mensaje) {}
