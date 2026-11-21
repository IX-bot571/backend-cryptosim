package com.proyecto.ms_precios.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/** Toda la configuración propia del servicio (prefijo {@code app}). Ninguna clase escribe nombres a mano. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotEmpty List<@Valid Cripto> catalogo,
        @NotNull @Valid Precios precios,
        @NotNull @Valid Historial historial,
        @NotNull @Valid Binance binance,
        @NotNull @Valid Kafka kafka) {

    public record Cripto(@NotBlank String simbolo, @NotBlank String nombre,
                         @NotBlank String par, @NotNull @Positive BigDecimal precioBase) {}

    public enum Modo { BINANCE, SIMULADO, BINANCE_CON_RESPALDO }

    public record Precios(@NotNull Modo modo, @Positive long intervaloMs,
                          @NotNull @Positive BigDecimal volatilidadPct) {}

    public record Historial(@Positive int maxPuntos) {}

    public record Binance(@NotBlank String url, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {}

    public record Kafka(@NotNull @Valid Topics topics) {
        public record Topics(@NotNull @Valid Topico precios) {}
    }

    public record Topico(@NotBlank String nombre, @Positive int particiones, @Positive int replicas,
                         @Positive int minInsyncReplicas, @Positive long retencionMs, @NotBlank String cleanupPolicy) {}
}
