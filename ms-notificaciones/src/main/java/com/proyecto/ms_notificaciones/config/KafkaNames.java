package com.proyecto.ms_notificaciones.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Nombres y parámetros de Kafka (app.kafka.*). Única fuente de verdad, igual que RabbitNames para RabbitMQ. */
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaNames(@NotNull @Valid Groups groups, @NotNull @Valid Topics topics,
                         @NotNull @Valid Reintentos reintentos) {

    public record Groups(@NotBlank String notificaciones, @NotBlank String notificacionesDlt) {}

    public record Topics(@NotNull @Valid TopicoConsumido alertasDisparadas, @NotNull @Valid Topico alertasDisparadasDlt) {}

    public record TopicoConsumido(@NotBlank String nombre) {}

    public record Topico(@NotBlank String nombre, @Positive int particiones, @Positive int replicas,
                         @Positive int minInsyncReplicas, @Positive long retencionMs, @NotBlank String cleanupPolicy) {}

    public record Reintentos(@Positive int maxReintentos, @Positive long intervaloInicialMs,
                             @Positive double multiplicador, @Positive long intervaloMaximoMs) {}
}
