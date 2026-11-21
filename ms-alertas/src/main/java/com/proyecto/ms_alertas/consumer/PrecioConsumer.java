package com.proyecto.ms_alertas.consumer;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.service.AlertaEvaluadorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Infraestructura: recibe de precios.actualizados, valida y delega. Sin lógica de negocio. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrecioConsumer {

    private final PrecioEventValidator validator;
    private final AlertaEvaluadorService evaluador;

    @KafkaListener(topics = "${app.kafka.topics.precios.nombre}", groupId = "${app.kafka.groups.alertas}")
    public void recibir(ConsumerRecord<String, PrecioEvent> registro) {
        log.debug("Precio recibido topico={} particion={} offset={} clave={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key());
        validator.validar(registro.value());
        evaluador.evaluar(registro.value());
    }
}
