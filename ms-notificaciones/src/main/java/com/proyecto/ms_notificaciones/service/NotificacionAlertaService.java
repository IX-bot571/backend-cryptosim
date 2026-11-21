package com.proyecto.ms_notificaciones.service;

import com.proyecto.ms_notificaciones.dto.AlertaDisparadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** "Envía" la notificación de una alerta disparada. Simulado: log estructurado. No conoce Kafka. */
@Slf4j
@Service
public class NotificacionAlertaService {

    public void notificar(AlertaDisparadaEvent e) {
        log.info("NOTIFICACION ALERTA usuario={} alertaId={} simbolo={} tipo={} objetivo={} precio={} hora={} mensaje=\"{}\"",
                e.usuarioId(), e.alertaId(), e.simbolo(), e.tipo(), e.valorObjetivo(), e.precioDisparo(),
                e.timestamp(), e.mensaje());
    }
}
