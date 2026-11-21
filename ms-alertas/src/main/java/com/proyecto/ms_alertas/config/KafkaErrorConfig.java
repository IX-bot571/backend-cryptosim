package com.proyecto.ms_alertas.config;

import com.proyecto.ms_alertas.exception.PayloadInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores del consumidor de precios:
 * - Reintentos con backoff exponencial (1 s, 2 s, 4 s) para fallos transitorios (por ejemplo, BD caída).
 * - Sin reintentos para lo que nunca va a funcionar: JSON ilegible o payload inválido.
 * - Agotados los reintentos (o si no es reintentable) el registro va a precios.actualizados.DLT con los
 *   headers de excepción (clase, mensaje, tópico/partición/offset originales) y el consumo sigue.
 * Boot inyecta este CommonErrorHandler en la factory de listeners por defecto.
 */
@Slf4j
@Configuration
public class KafkaErrorConfig {

    /** Los DLT se declaran donde está el error handler. 1 partición: el destino usa partición -1 (ver abajo). */
    @Bean
    NewTopic preciosDltTopic(AppProperties props) {
        AppProperties.Topico t = props.kafka().topics().preciosDlt();
        return TopicBuilder.name(t.nombre())
                .partitions(t.particiones())
                .replicas(t.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retencionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()))
                .build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> jsonTemplate,
                                          ProducerFactory<Object, Object> producerFactory,
                                          AppProperties props) {
        Map<String, String> dltPorTopico = Map.of(
                props.kafka().topics().precios().nombre(), props.kafka().topics().preciosDlt().nombre());

        // Si falló la deserialización, el valor es el byte[] original: se reenvía tal cual con ByteArraySerializer.
        // Si falló la validación, el valor es un PrecioEvent: se reenvía como JSON con el template normal.
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, new KafkaTemplate<>(producerFactory.copyWithConfigurationOverride(
                Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class))));
        templates.put(Object.class, jsonTemplate);

        // Partición -1: decide el productor. Por defecto se usaría la misma partición del original y el DLT tiene 1.
        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(templates,
                (registro, ex) -> new TopicPartition(dltPara(dltPorTopico, registro), -1));

        ConsumerRecordRecoverer recovererConLog = (registro, ex) -> {
            log.error("Enviando a DLT destino={} topico={} particion={} offset={} clave={} causa={}",
                    dltPara(dltPorTopico, registro), registro.topic(), registro.partition(), registro.offset(),
                    registro.key(), causa(ex));
            // Se publica la causa real (por ejemplo PayloadInvalidoException) y no el envoltorio del listener,
            // así los headers kafka_dlt-exception-fqcn/message del DLT dicen qué falló.
            dlt.accept(registro, ex instanceof ListenerExecutionFailedException lefe && lefe.getCause() instanceof Exception c
                    ? c : ex);
        };

        AppProperties.Reintentos r = props.kafka().reintentos();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(r.maxReintentos());
        backOff.setInitialInterval(r.intervaloInicialMs());
        backOff.setMultiplier(r.multiplicador());
        backOff.setMaxInterval(r.intervaloMaximoMs());

        DefaultErrorHandler handler = new DefaultErrorHandler(recovererConLog, backOff);
        handler.addNotRetryableExceptions(DeserializationException.class, PayloadInvalidoException.class);
        handler.setRetryListeners((registro, ex, intento) ->
                log.warn("Fallo procesando registro (intento {}) topico={} particion={} offset={} clave={} causa={}",
                        intento, registro.topic(), registro.partition(), registro.offset(), registro.key(), causa(ex)));
        return handler;
    }

    private static String dltPara(Map<String, String> dltPorTopico, ConsumerRecord<?, ?> registro) {
        String destino = dltPorTopico.get(registro.topic());
        if (destino == null) {
            throw new IllegalStateException("No hay DLT configurado para el tópico " + registro.topic());
        }
        return destino;
    }

    private static String causa(Exception ex) {
        Throwable c = NestedExceptionUtils.getMostSpecificCause(ex);
        return c.getClass().getSimpleName() + ": " + c.getMessage();
    }
}
