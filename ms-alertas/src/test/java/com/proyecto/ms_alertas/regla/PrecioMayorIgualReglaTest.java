package com.proyecto.ms_alertas.regla;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.Alerta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecioMayorIgualReglaTest {

    private final PrecioMayorIgualRegla regla = new PrecioMayorIgualRegla();

    private static Alerta alerta(String valor) {
        Alerta a = new Alerta();
        a.setSimbolo("BTC");
        a.setValor(new BigDecimal(valor));
        return a;
    }

    private static PrecioEvent precio(String valor) {
        return new PrecioEvent("BTC", new BigDecimal(valor), null, null, null, null, Instant.now(), "SIMULADO");
    }

    @Test
    void cumple() {
        assertTrue(regla.cumple(alerta("100"), precio("100.01")));
    }

    @Test
    void noCumple() {
        assertFalse(regla.cumple(alerta("100"), precio("99.99")));
    }

    @Test
    void bordeExactoCumpleAunqueCambieLaEscala() {
        assertTrue(regla.cumple(alerta("100"), precio("100.00000000")));
    }
}
