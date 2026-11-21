package com.proyecto.ms_alertas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seguimientos", uniqueConstraints =
        @UniqueConstraint(name = "uk_seguimiento_usuario_simbolo", columnNames = {"usuarioId", "simbolo"}))
@Getter
@Setter
@NoArgsConstructor
public class Seguimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false, length = 20)
    private String simbolo;

    public Seguimiento(String usuarioId, String simbolo) {
        this.usuarioId = usuarioId;
        this.simbolo = simbolo;
    }
}
