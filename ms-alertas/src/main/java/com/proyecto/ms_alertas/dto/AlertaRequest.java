package com.proyecto.ms_alertas.dto;

import com.proyecto.ms_alertas.entity.TipoAlerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Cuerpo de POST /api/alertas. El usuario NO viene aquí: sale del JWT. */
public record AlertaRequest(@NotBlank String simbolo, @NotNull TipoAlerta tipo, @NotNull @Positive BigDecimal valor) {}
