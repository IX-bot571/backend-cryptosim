package com.proyecto.ms_admin_kafka.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Cuerpo de PUT /api/topics/{name}/config. Un valor null elimina la config (vuelve al valor por defecto). */
public record ConfigRequest(@NotEmpty Map<String, String> configs) {}
