package com.proyecto.ms_alertas.dto;

import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.Direccion;
import com.proyecto.ms_alertas.entity.EstadoAlerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertaResponse(Long id, String simbolo, TipoAlerta tipo, BigDecimal valor, EstadoAlerta estado,
                             Instant creadaEn, Instant disparadaEn, BigDecimal precioDisparo,
                             Direccion direccion, BigDecimal porcentaje, Integer ventanaMinutos) {

    public static AlertaResponse de(Alerta a) {
        return new AlertaResponse(a.getId(), a.getSimbolo(), a.getTipo(), a.getValor(), a.getEstado(),
                a.getCreadaEn(), a.getDisparadaEn(), a.getPrecioDisparo(),
                a.getDireccion(), a.getPorcentaje(), a.getVentanaMinutos());
    }
}
