package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.config.AppProperties;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import com.proyecto.ms_alertas.exception.AlertaInvalidaException;
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
    private final AppProperties props;

    public AlertaResponse crear(String usuarioId, AlertaRequest request) {
        Alerta a = new Alerta();
        a.setUsuarioId(usuarioId);
        a.setSimbolo(catalogo.validar(request.simbolo()));
        a.setTipo(request.tipo());
        if (request.tipo() == TipoAlerta.VARIACION_VENTANA) {
            int max = props.ventana().maxMinutos();
            if (request.direccion() == null || request.porcentaje() == null || request.ventanaMinutos() == null) {
                throw new AlertaInvalidaException("VARIACION_VENTANA requiere direccion, porcentaje y ventanaMinutos");
            }
            if (request.ventanaMinutos() > max) {
                throw new AlertaInvalidaException("ventanaMinutos no puede superar " + max
                        + " (retención de precios.actualizados)");
            }
            a.setDireccion(request.direccion());
            a.setPorcentaje(request.porcentaje());
            a.setVentanaMinutos(request.ventanaMinutos());
        } else {
            if (request.valor() == null) {
                throw new AlertaInvalidaException(request.tipo() + " requiere valor");
            }
            a.setValor(request.valor());
        }
        Alerta guardada = repository.save(a);
        log.info("Alerta creada id={} usuario={} tipo={} {}", guardada.getId(), usuarioId,
                guardada.getTipo(), Descripciones.condicion(guardada));
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
