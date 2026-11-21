package com.proyecto.ms_notificaciones.consumer;

import com.proyecto.ms_notificaciones.dto.AlertaDisparadaEvent;
import com.proyecto.ms_notificaciones.service.NotificacionAlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Infraestructura: recibe de alertas.disparadas, valida y delega. Sin lógica de negocio. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertaDisparadaConsumer {

    private final AlertaDisparadaValidator validator;
    private final NotificacionAlertaService service;

    @KafkaListener(topics = "${app.kafka.topics.alertas-disparadas.nombre}",
            groupId = "${app.kafka.groups.notificaciones}")
    public void recibir(ConsumerRecord<String, AlertaDisparadaEvent> registro) {
        log.info("Alerta recibida topico={} particion={} offset={} clave={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key());
        validator.validar(registro.value());
        service.notificar(registro.value());
    }
}
