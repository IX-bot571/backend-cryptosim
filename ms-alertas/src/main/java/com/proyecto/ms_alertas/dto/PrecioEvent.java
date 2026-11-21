package com.proyecto.ms_alertas.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento consumido de precios.actualizados (lo publica ms-precios; mismo JSON, ver CONTRATO.md). */
public record PrecioEvent(String simbolo, BigDecimal precio, BigDecimal variacion24hPct,
                          BigDecimal maximo24h, BigDecimal minimo24h, BigDecimal volumen24h,
                          Instant timestamp, String fuente) {}
