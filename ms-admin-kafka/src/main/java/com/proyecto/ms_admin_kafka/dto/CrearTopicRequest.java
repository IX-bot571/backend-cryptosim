package com.proyecto.ms_admin_kafka.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/** Cuerpo de POST /api/topics. configs es opcional (por ejemplo retention.ms, cleanup.policy, min.insync.replicas). */
public record CrearTopicRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "solo letras, números, '.', '_' y '-'") String name,
        @NotNull @Min(1) Integer particiones,
        @NotNull @Min(1) @Max(Short.MAX_VALUE) Integer replicas,
        Map<String, String> configs) {}
