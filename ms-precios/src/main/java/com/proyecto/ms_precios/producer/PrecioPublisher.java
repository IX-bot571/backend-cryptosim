package com.proyecto.ms_precios.producer;

import com.proyecto.ms_precios.config.AppProperties;
import com.proyecto.ms_precios.dto.PrecioEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publica en precios.actualizados con clave = simbolo: cada cripto cae siempre en la misma partición (orden). */
@Slf4j
@Component
public class PrecioPublisher {

    private final KafkaTemplate<String, PrecioEvent> kafkaTemplate;
    private final String topico;

    public PrecioPublisher(KafkaTemplate<String, PrecioEvent> kafkaTemplate, AppProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.topico = props.kafka().topics().precios().nombre();
    }

    public void publicar(PrecioEvent evento) {
        String clave = evento.simbolo();
        try {
            kafkaTemplate.send(topico, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("Error publicando precio topico={} clave={} causa={}", topico, clave, error.getMessage());
                } else {
                    RecordMetadata m = resultado.getRecordMetadata();
                    log.debug("Precio publicado topico={} particion={} offset={} clave={} precio={} fuente={}",
                            m.topic(), m.partition(), m.offset(), clave, evento.precio(), evento.fuente());
                }
            });
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el precio topico={} clave={} causa={}", topico, clave, e.getMessage());
        }
    }
}
