package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.exception.FalloTransitorioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gancho de prueba para la demo de reintentos (desactivado por defecto).
 * Con app.demo.fallo-transitorio.simbolo=BTC y veces=2, los 2 primeros precios de BTC fallan con una excepción
 * reintentable y el tercer intento funciona (backoff 1 s, 2 s). Con veces >= 4 se agotan los 3 reintentos y el
 * registro va al DLT. Se consume una sola vez: para repetir, reiniciar el servicio.
 */
@Slf4j
@Component
public class FalloTransitorioSimulado {

    private final String simbolo;
    private final AtomicInteger fallosRestantes;

    public FalloTransitorioSimulado(@Value("${app.demo.fallo-transitorio.simbolo:}") String simbolo,
                                    @Value("${app.demo.fallo-transitorio.veces:0}") int veces) {
        this.simbolo = simbolo;
        this.fallosRestantes = new AtomicInteger(simbolo.isBlank() ? 0 : veces);
        if (fallosRestantes.get() > 0) {
            log.warn("GANCHO DE PRUEBA ACTIVO: los próximos {} precios de {} fallarán de forma transitoria", veces, simbolo);
        }
    }

    public void quizasFallar(String simboloPrecio) {
        if (simbolo.equals(simboloPrecio) && fallosRestantes.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            throw new FalloTransitorioException("Fallo transitorio simulado para " + simboloPrecio);
        }
    }
}
