package com.proyecto.ms_alertas.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Catálogo y nombres de Kafka (tópicos, grupos) leídos de application.yml. Ninguna clase los escribe a mano. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotEmpty List<@Valid Cripto> catalogo, @NotNull @Valid Kafka kafka,
                            @NotNull @Valid Ventana ventana) {

    /** Límite de ventanaMinutos en VARIACION_VENTANA (no más que la retención de precios.actualizados). */
    public record Ventana(@Positive int maxMinutos) {}

    public record Cripto(@NotBlank String simbolo, @NotBlank String nombre) {}

    public record Kafka(@NotNull @Valid Groups groups, @NotNull @Valid Topics topics,
                        @NotNull @Valid Reintentos reintentos) {}

    public record Groups(@NotBlank String alertas, @NotBlank String alertasDlt) {}

    public record Topics(@NotNull @Valid TopicoConsumido precios, @NotNull @Valid Topico preciosDlt,
                         @NotNull @Valid Topico alertasDisparadas) {}

    public record Reintentos(@Positive int maxReintentos, @Positive long intervaloInicialMs,
                             @Positive double multiplicador, @Positive long intervaloMaximoMs) {}

    public record TopicoConsumido(@NotBlank String nombre) {}

    public record Topico(@NotBlank String nombre, @Positive int particiones, @Positive int replicas,
                         @Positive int minInsyncReplicas, @Positive long retencionMs, @NotBlank String cleanupPolicy) {}
}
