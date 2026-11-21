package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.entity.Alerta;

/** Textos legibles de una alerta, por ejemplo "BTC ≥ 60000". */
final class Descripciones {

    private Descripciones() {
    }

    static String condicion(Alerta a) {
        String op = switch (a.getTipo()) {
            case PRECIO_MAYOR_IGUAL -> "≥";
            case PRECIO_MENOR_IGUAL -> "≤";
        };
        return a.getSimbolo() + " " + op + " " + a.getValor().stripTrailingZeros().toPlainString();
    }
}
