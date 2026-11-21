package com.proyecto.ms_precios.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Evento publicado en precios.actualizados (clave = simbolo). fuente: BINANCE | SIMULADO. */
public record PrecioEvent(String simbolo, BigDecimal precio, BigDecimal variacion24hPct,
                          BigDecimal maximo24h, BigDecimal minimo24h, BigDecimal volumen24h,
                          Instant timestamp, String fuente) {

    public static final String FUENTE_BINANCE = "BINANCE";
    public static final String FUENTE_SIMULADO = "SIMULADO";
}
