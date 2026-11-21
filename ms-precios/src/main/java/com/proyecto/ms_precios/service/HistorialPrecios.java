package com.proyecto.ms_precios.service;

import com.proyecto.ms_precios.config.AppProperties;
import com.proyecto.ms_precios.dto.PrecioEvent;
import com.proyecto.ms_precios.dto.PuntoHistorial;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Último precio y buffer circular en memoria por cripto (máximo app.historial.max-puntos puntos). */
@Service
public class HistorialPrecios {

    private final int maxPuntos;
    private final Map<String, PrecioEvent> ultimos = new ConcurrentHashMap<>();
    private final Map<String, Deque<PuntoHistorial>> historial = new ConcurrentHashMap<>();

    public HistorialPrecios(AppProperties props) {
        this.maxPuntos = props.historial().maxPuntos();
    }

    public void registrar(PrecioEvent e) {
        ultimos.put(e.simbolo(), e);
        Deque<PuntoHistorial> puntos = historial.computeIfAbsent(e.simbolo(), s -> new ArrayDeque<>(maxPuntos));
        synchronized (puntos) {
            if (puntos.size() >= maxPuntos) {
                puntos.pollFirst();
            }
            puntos.addLast(new PuntoHistorial(e.timestamp(), e.precio()));
        }
    }

    /** Último precio de cada símbolo, en el orden recibido (el del catálogo). */
    public List<PrecioEvent> ultimos(List<String> simbolos) {
        return simbolos.stream().map(ultimos::get).filter(Objects::nonNull).toList();
    }

    public List<PuntoHistorial> historial(String simbolo, int minutos) {
        Deque<PuntoHistorial> puntos = historial.get(simbolo);
        if (puntos == null) {
            return List.of();
        }
        Instant desde = Instant.now().minus(Duration.ofMinutes(minutos));
        synchronized (puntos) {
            return puntos.stream().filter(p -> !p.timestamp().isBefore(desde)).toList();
        }
    }
}
