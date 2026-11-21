package com.proyecto.ms_precios.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Campos usados de GET /api/v3/ticker/24hr (Binance los envía como texto; Jackson los convierte a BigDecimal). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceTicker(String symbol, BigDecimal lastPrice, BigDecimal priceChangePercent,
                            BigDecimal highPrice, BigDecimal lowPrice, BigDecimal volume) {}
