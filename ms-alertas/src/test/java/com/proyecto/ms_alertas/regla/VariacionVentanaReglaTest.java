package com.proyecto.ms_alertas.regla;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.Direccion;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import com.proyecto.ms_alertas.service.VentanaPrecios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ventana de 10 min, 1 %. Referencia = primer precio con timestamp ≥ ahora − 10 min. */
class VariacionVentanaReglaTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");

    private VentanaPrecios ventana;
    private VariacionVentanaRegla regla;

    @BeforeEach
    void setUp() {
        ventana = new VentanaPrecios();
        regla = new VariacionVentanaRegla(ventana);
        // Fuera de la ventana (no cuenta), y la referencia real: 100 hace 9 min.
        registrar(AHORA.minus(Duration.ofMinutes(15)), "50");
        registrar(AHORA.minus(Duration.ofMinutes(9)), "100");
        registrar(AHORA.minus(Duration.ofMinutes(5)), "100.5");
    }

    private void registrar(Instant t, String precio) {
        ventana.registrar("BTC", t, new BigDecimal(precio), Duration.ofMinutes(60));
    }

    private static Alerta alerta(Direccion direccion) {
        Alerta a = new Alerta();
        a.setSimbolo("BTC");
        a.setTipo(TipoAlerta.VARIACION_VENTANA);
        a.setDireccion(direccion);
        a.setPorcentaje(new BigDecimal("1"));
        a.setVentanaMinutos(10);
        return a;
    }

    private PrecioEvent precio(String valor) {
        registrar(AHORA, valor); // como hace el evaluador: registra y luego evalúa
        return new PrecioEvent("BTC", new BigDecimal(valor), null, null, null, null, AHORA, "SIMULADO");
    }

    @Test
    void subeCumple() {
        assertTrue(regla.cumple(alerta(Direccion.SUBE), precio("101.5")));
    }

    @Test
    void subeNoCumple() {
        assertFalse(regla.cumple(alerta(Direccion.SUBE), precio("100.99")));
    }

    @Test
    void subeBordeExacto() {
        assertTrue(regla.cumple(alerta(Direccion.SUBE), precio("101.00000000")));
    }

    @Test
    void bajaCumpleYSubidaNoLaDispara() {
        assertTrue(regla.cumple(alerta(Direccion.BAJA), precio("98.9")));
        assertFalse(new VariacionVentanaRegla(ventana).cumple(alerta(Direccion.BAJA),
                new PrecioEvent("BTC", new BigDecimal("102"), null, null, null, null, AHORA, "SIMULADO")));
    }

    @Test
    void bajaBordeExacto() {
        assertTrue(regla.cumple(alerta(Direccion.BAJA), precio("99")));
    }

    @Test
    void ambasCumpleEnCualquierSentidoYDiceCual() {
        Alerta a = alerta(Direccion.AMBAS);
        PrecioEvent p = precio("98.5");
        assertTrue(regla.cumple(a, p));
        assertEquals("bajó 1.50 % (de 100 a 98.5)", regla.detalle(a, p));
    }

    @Test
    void ambasNoCumpleConMovimientoChico() {
        assertFalse(regla.cumple(alerta(Direccion.AMBAS), precio("100.5")));
    }

    @Test
    void sinHistorialNoCumple() {
        VariacionVentanaRegla vacia = new VariacionVentanaRegla(new VentanaPrecios());
        assertFalse(vacia.cumple(alerta(Direccion.AMBAS),
                new PrecioEvent("ETH", new BigDecimal("1"), null, null, null, null, AHORA, "SIMULADO")));
    }

    @Test
    void ventanaDescartaPuntosViejos() {
        VentanaPrecios v = new VentanaPrecios();
        v.registrar("BTC", AHORA.minus(Duration.ofMinutes(30)), BigDecimal.ONE, Duration.ofMinutes(10));
        v.registrar("BTC", AHORA, BigDecimal.TEN, Duration.ofMinutes(10));
        assertEquals(1, v.tamano("BTC"));
    }
}
