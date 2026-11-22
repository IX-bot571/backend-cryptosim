package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;

import java.math.BigDecimal;

/** Textos legibles de una alerta, por ejemplo "BTC ≥ 60000" o "BTC sube 1 % en 120 min". */
final class Descripciones {

    private Descripciones() {
    }

    static String condicion(Alerta a) {
        return switch (a.getTipo()) {
            case PRECIO_MAYOR_IGUAL -> a.getSimbolo() + " ≥ " + texto(a.getValor());
            case PRECIO_MENOR_IGUAL -> a.getSimbolo() + " ≤ " + texto(a.getValor());
            case VARIACION_VENTANA -> a.getSimbolo() + " " + switch (a.getDireccion()) {
                case SUBE -> "sube";
                case BAJA -> "baja";
                case AMBAS -> "se mueve ±";
            } + " " + texto(a.getPorcentaje()) + " % en " + a.getVentanaMinutos() + " min";
        };
    }

    /** valorObjetivo del evento: el precio objetivo o, en VARIACION_VENTANA, el porcentaje. */
    static BigDecimal objetivo(Alerta a) {
        return a.getTipo() == TipoAlerta.VARIACION_VENTANA ? a.getPorcentaje() : a.getValor();
    }

    private static String texto(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
