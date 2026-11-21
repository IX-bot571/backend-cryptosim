package com.proyecto.ms_alertas.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Registra en el log cada mensaje que llega a precios.actualizados.DLT, con la causa y su origen.
 * El valor se lee como texto (StringDeserializer) porque puede ser JSON inválido.
 */
@Slf4j
@Component
public class PrecioDltConsumer {

    @KafkaListener(topics = "${app.kafka.topics.precios-dlt.nombre}", groupId = "${app.kafka.groups.alertas-dlt}",
            properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "auto.offset.reset=earliest"})
    public void registrar(ConsumerRecord<String, String> registro) {
        log.error("Mensaje en DLT topico={} particion={} offset={} clave={} | origen={}-{}@{} | excepcion={} | mensaje={} | valor={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                texto(registro, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                entero(registro, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                largo(registro, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                texto(registro, KafkaHeaders.DLT_EXCEPTION_FQCN),
                texto(registro, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                registro.value());
    }

    private static String texto(ConsumerRecord<?, ?> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h == null ? "-" : new String(h.value(), StandardCharsets.UTF_8);
    }

    private static String entero(ConsumerRecord<?, ?> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h == null || h.value().length != Integer.BYTES ? "-" : String.valueOf(ByteBuffer.wrap(h.value()).getInt());
    }

    private static String largo(ConsumerRecord<?, ?> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h == null || h.value().length != Long.BYTES ? "-" : String.valueOf(ByteBuffer.wrap(h.value()).getLong());
    }
}
