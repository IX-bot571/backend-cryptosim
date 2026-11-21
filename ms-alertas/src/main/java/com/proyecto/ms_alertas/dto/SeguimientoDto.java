package com.proyecto.ms_alertas.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Cuerpo y respuesta de /api/seguimiento. */
public record SeguimientoDto(@NotNull List<String> simbolos) {}
