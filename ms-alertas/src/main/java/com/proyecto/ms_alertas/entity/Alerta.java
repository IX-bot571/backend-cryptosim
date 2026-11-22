package com.proyecto.ms_alertas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "alertas", indexes = {
        @Index(name = "idx_alertas_simbolo_estado", columnList = "simbolo, estado"),
        @Index(name = "idx_alertas_usuario", columnList = "usuarioId")})
@Getter
@Setter
@NoArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Claim sub del JWT. */
    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false, length = 20)
    private String simbolo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAlerta tipo;

    /** Precio objetivo de PRECIO_MAYOR_IGUAL / PRECIO_MENOR_IGUAL. Null en VARIACION_VENTANA. */
    @Column(precision = 24, scale = 8)
    private BigDecimal valor;

    // --- Solo VARIACION_VENTANA: "avísame si <simbolo> <direccion> <porcentaje>% en <ventanaMinutos> min" ---

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Direccion direccion;

    /** Siempre positivo; la dirección indica el signo. */
    @Column(precision = 10, scale = 4)
    private BigDecimal porcentaje;

    private Integer ventanaMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlerta estado = EstadoAlerta.ACTIVA;

    @Column(nullable = false)
    private Instant creadaEn = Instant.now();

    private Instant disparadaEn;

    @Column(precision = 24, scale = 8)
    private BigDecimal precioDisparo;
}
