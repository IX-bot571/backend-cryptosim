package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.dto.AlertaDisparadaEvent;
import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.EstadoAlerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import com.proyecto.ms_alertas.producer.AlertaDisparadaPublisher;
import com.proyecto.ms_alertas.regla.ReglaAlerta;
import com.proyecto.ms_alertas.repository.AlertaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio: dado un precio, dispara las alertas ACTIVAS del símbolo que cumplen su regla.
 * No conoce Kafka: recibe un PrecioEvent ya validado.
 */
@Slf4j
@Service
public class AlertaEvaluadorService {

    private final AlertaRepository repository;
    private final AlertaDisparadaPublisher publisher;
    private final FalloTransitorioSimulado falloSimulado;
    private final VentanaPrecios ventana;
    private final Map<TipoAlerta, ReglaAlerta> reglas = new EnumMap<>(TipoAlerta.class);

    public AlertaEvaluadorService(AlertaRepository repository, AlertaDisparadaPublisher publisher,
                                  FalloTransitorioSimulado falloSimulado, VentanaPrecios ventana,
                                  List<ReglaAlerta> reglas) {
        this.repository = repository;
        this.publisher = publisher;
        this.falloSimulado = falloSimulado;
        this.ventana = ventana;
        reglas.forEach(r -> this.reglas.put(r.tipo(), r));
    }

    public void evaluar(PrecioEvent precio) {
        falloSimulado.quizasFallar(precio.simbolo());
        List<Alerta> activas = repository.findBySimboloAndEstado(precio.simbolo(), EstadoAlerta.ACTIVA);
        // Se registra antes de evaluar; se retiene solo la ventana más larga activa de este símbolo.
        ventana.registrar(precio.simbolo(), precio.timestamp(), precio.precio(), ventanaMasLarga(activas));
        for (Alerta alerta : activas) {
            ReglaAlerta regla = reglas.get(alerta.getTipo());
            if (regla == null) {
                log.warn("Sin regla para el tipo {} (alerta id={})", alerta.getTipo(), alerta.getId());
                continue;
            }
            if (regla.cumple(alerta, precio)) {
                disparar(alerta, precio, regla.detalle(alerta, precio));
            }
        }
    }

    static Duration ventanaMasLarga(List<Alerta> alertas) {
        return Duration.ofMinutes(alertas.stream()
                .filter(a -> a.getTipo() == TipoAlerta.VARIACION_VENTANA && a.getVentanaMinutos() != null)
                .mapToInt(Alerta::getVentanaMinutos).max().orElse(0));
    }

    private void disparar(Alerta alerta, PrecioEvent precio, String detalle) {
        Instant ahora = Instant.now();
        int filas = repository.marcarDisparada(alerta.getId(), precio.precio(), ahora,
                EstadoAlerta.ACTIVA, EstadoAlerta.DISPARADA);
        if (filas == 0) {
            log.info("Alerta id={} ya estaba disparada (otra instancia o reintento): no se publica de nuevo",
                    alerta.getId());
            return;
        }
        String condicion = Descripciones.condicion(alerta);
        String mensaje = condicion + " · " + (detalle != null ? detalle
                : "precio " + precio.precio().stripTrailingZeros().toPlainString());
        log.info("Alerta DISPARADA id={} usuario={} {}", alerta.getId(), alerta.getUsuarioId(), mensaje);
        // Se publica después de confirmar el UPDATE: si la transacción falla no sale ningún evento.
        publisher.publicar(new AlertaDisparadaEvent(alerta.getId(), alerta.getUsuarioId(), alerta.getSimbolo(),
                alerta.getTipo().name(), Descripciones.objetivo(alerta), precio.precio(), ahora, mensaje));
    }
}
