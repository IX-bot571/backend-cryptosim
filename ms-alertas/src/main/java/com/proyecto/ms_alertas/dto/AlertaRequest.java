package com.proyecto.ms_alertas.dto;

import com.proyecto.ms_alertas.entity.Direccion;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Cuerpo de POST /api/alertas. El usuario NO viene aquí: sale del JWT.
 * PRECIO_MAYOR_IGUAL / PRECIO_MENOR_IGUAL usan {@code valor}; VARIACION_VENTANA usa
 * {@code direccion}, {@code porcentaje} y {@code ventanaMinutos}. La coherencia por tipo se valida en AlertaService.
 */
public record AlertaRequest(@NotBlank String simbolo, @NotNull TipoAlerta tipo, @Positive BigDecimal valor,
                            Direccion direccion, @Positive BigDecimal porcentaje, @Min(1) Integer ventanaMinutos) {}
