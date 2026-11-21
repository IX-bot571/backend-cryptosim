package com.proyecto.ms_precios.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PuntoHistorial(Instant timestamp, BigDecimal precio) {}
