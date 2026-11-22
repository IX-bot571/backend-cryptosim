package com.proyecto.ms_alertas.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado en memoria para las alertas VARIACION_VENTANA: por símbolo, una cola de (timestamp, precio)
 * ordenada por tiempo. Se descarta lo que queda fuera de la ventana más larga que se necesita.
 * Usa el timestamp del evento (no el reloj local), así la relectura de Kafka al arrancar da el mismo resultado.
 * Se pierde al reiniciar; {@code VentanaReplay} la reconstruye desde precios.actualizados.
 */
@Component
public class VentanaPrecios {

    private record Punto(Instant timestamp, BigDecimal precio) {}

    private final Map<String, Deque<Punto>> porSimbolo = new ConcurrentHashMap<>();

    /**
     * Agrega un precio y descarta los puntos anteriores a {@code timestamp - retener}.
     * Ignora puntos más viejos que el último (llegan ordenados por partición; esto protege de duplicados).
     */
    public void registrar(String simbolo, Instant timestamp, BigDecimal precio, Duration retener) {
        Deque<Punto> cola = porSimbolo.computeIfAbsent(simbolo, s -> new ArrayDeque<>());
        synchronized (cola) {
            Punto ultimo = cola.peekLast();
            if (ultimo == null || timestamp.isAfter(ultimo.timestamp())) {
                cola.addLast(new Punto(timestamp, precio));
            }
            Instant limite = timestamp.minus(retener);
            while (cola.size() > 1 && cola.peekFirst().timestamp().isBefore(limite)) {
                cola.pollFirst();
            }
        }
    }

    /** Precio de referencia: el primer punto con timestamp ≥ {@code desde}. Vacío si no hay ninguno. */
    public Optional<BigDecimal> referencia(String simbolo, Instant desde) {
        Deque<Punto> cola = porSimbolo.get(simbolo);
        if (cola == null) {
            return Optional.empty();
        }
        synchronized (cola) {
            for (Punto p : cola) {
                if (!p.timestamp().isBefore(desde)) {
                    return Optional.of(p.precio());
                }
            }
        }
        return Optional.empty();
    }

    public int tamano(String simbolo) {
        Deque<Punto> cola = porSimbolo.get(simbolo);
        if (cola == null) {
            return 0;
        }
        synchronized (cola) {
            return cola.size();
        }
    }
}
