package com.proyecto.ms_admin_kafka.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de PUT /api/topics/{name}/partitions: número TOTAL de particiones (solo se puede aumentar). */
public record ParticionesRequest(@NotNull @Min(1) Integer particiones) {}
