package com.proyecto.ms_alertas.producer;

import com.proyecto.ms_alertas.config.AppProperties;
import com.proyecto.ms_alertas.dto.AlertaDisparadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publica en alertas.disparadas con clave = usuarioId: las alertas de un usuario conservan su orden. */
@Slf4j
@Component
public class AlertaDisparadaPublisher {

    private final KafkaTemplate<String, AlertaDisparadaEvent> kafkaTemplate;
    private final String topico;

    public AlertaDisparadaPublisher(KafkaTemplate<String, AlertaDisparadaEvent> kafkaTemplate, AppProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.topico = props.kafka().topics().alertasDisparadas().nombre();
    }

    public void publicar(AlertaDisparadaEvent evento) {
        String clave = evento.usuarioId();
        try {
            kafkaTemplate.send(topico, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("Error publicando alerta topico={} clave={} alertaId={} causa={}",
                            topico, clave, evento.alertaId(), error.getMessage());
                } else {
                    RecordMetadata m = resultado.getRecordMetadata();
                    log.info("Alerta publicada topico={} particion={} offset={} clave={} alertaId={}",
                            m.topic(), m.partition(), m.offset(), clave, evento.alertaId());
                }
            });
        } catch (RuntimeException e) {
            log.error("No se pudo enviar la alerta topico={} clave={} alertaId={} causa={}",
                    topico, clave, evento.alertaId(), e.getMessage());
        }
    }
}
