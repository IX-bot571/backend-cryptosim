package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.dto.AlertaRequest;
import com.proyecto.ms_alertas.dto.AlertaResponse;
import com.proyecto.ms_alertas.dto.NotificacionMensaje;
import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.EstadoAlerta;
import com.proyecto.ms_alertas.exception.AlertaNoCancelableException;
import com.proyecto.ms_alertas.exception.AlertaNoEncontradaException;
import com.proyecto.ms_alertas.producer.NotificacionProducer;
import com.proyecto.ms_alertas.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** CRUD de alertas del usuario autenticado. El usuarioId siempre llega desde el JWT (sub). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository repository;
    private final CatalogoService catalogo;
    private final NotificacionProducer notificacionProducer;

    public AlertaResponse crear(String usuarioId, AlertaRequest request) {
        Alerta a = new Alerta();
        a.setUsuarioId(usuarioId);
        a.setSimbolo(catalogo.validar(request.simbolo()));
        a.setTipo(request.tipo());
        a.setValor(request.valor());
        Alerta guardada = repository.save(a);
        log.info("Alerta creada id={} usuario={} {} {} {}", guardada.getId(), usuarioId,
                guardada.getSimbolo(), guardada.getTipo(), guardada.getValor());
        // Flujo RabbitMQ existente: confirmación de alerta creada → notificaciones.queue
        notificacionProducer.enviar(new NotificacionMensaje(guardada.getId(), usuarioId,
                "Alerta creada: " + Descripciones.condicion(guardada)));
        return AlertaResponse.de(guardada);
    }

    public List<AlertaResponse> listar(String usuarioId, EstadoAlerta estado) {
        List<Alerta> alertas = estado == null
                ? repository.findByUsuarioIdOrderByCreadaEnDesc(usuarioId)
                : repository.findByUsuarioIdAndEstadoOrderByCreadaEnDesc(usuarioId, estado);
        return alertas.stream().map(AlertaResponse::de).toList();
    }

    public void cancelar(String usuarioId, Long id) {
        int filas = repository.marcarCancelada(id, usuarioId, EstadoAlerta.ACTIVA, EstadoAlerta.CANCELADA);
        if (filas == 1) {
            log.info("Alerta cancelada id={} usuario={}", id, usuarioId);
            return;
        }
        Alerta a = repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new AlertaNoEncontradaException(id));
        throw new AlertaNoCancelableException(id, a.getEstado());
    }
}
