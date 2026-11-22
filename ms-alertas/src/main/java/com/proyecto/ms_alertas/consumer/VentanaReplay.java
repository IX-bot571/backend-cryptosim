package com.proyecto.ms_alertas.consumer;

import com.proyecto.ms_alertas.config.AppProperties;
import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.EstadoAlerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import com.proyecto.ms_alertas.exception.PayloadInvalidoException;
import com.proyecto.ms_alertas.repository.AlertaRepository;
import com.proyecto.ms_alertas.service.VentanaPrecios;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Infraestructura: al arrancar, reconstruye la ventana de precios en memoria releyendo precios.actualizados
 * desde (ahora − ventana más larga activa), con offsetsForTimes. Los precios releídos solo alimentan la
 * ventana; no se evalúan alertas con ellos. Recién después arranca el listener normal de alertas-group.
 * Usa un consumidor sin grupo (assign, sin commits), así no altera los offsets de alertas-group.
 * Es posible gracias a la retención de 24 h del tópico.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VentanaReplay {

    private static final Duration LIMITE = Duration.ofSeconds(30);

    private final AlertaRepository repository;
    private final ConsumerFactory<?, ?> consumerFactory;
    private final KafkaListenerEndpointRegistry registry;
    private final PrecioEventValidator validator;
    private final VentanaPrecios ventana;
    private final AppProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void reconstruirYArrancar() {
        try {
            Integer minutos = repository.maxVentanaMinutos(TipoAlerta.VARIACION_VENTANA, EstadoAlerta.ACTIVA);
            if (minutos == null || minutos <= 0) {
                log.info("Sin alertas VARIACION_VENTANA activas: no hace falta releer precios");
            } else {
                releer(Duration.ofMinutes(minutos));
            }
        } catch (Exception e) {
            // Sin la relectura, la ventana se llena desde ahora: degradado pero funcional.
            log.warn("No se pudo reconstruir la ventana de precios: {}", e.toString());
        } finally {
            MessageListenerContainer contenedor = registry.getListenerContainer(PrecioConsumer.LISTENER_ID);
            if (contenedor != null && !contenedor.isRunning()) {
                contenedor.start();
                log.info("Listener {} arrancado", PrecioConsumer.LISTENER_ID);
            }
        }
    }

    private void releer(Duration retener) {
        String topico = props.kafka().topics().precios().nombre();
        Instant desde = Instant.now().minus(retener);
        try (Consumer<?, ?> consumer = consumerFactory.createConsumer(null, "ms-alertas-ventana", null)) {
            List<TopicPartition> particiones = consumer.partitionsFor(topico).stream()
                    .map(p -> new TopicPartition(topico, p.partition())).toList();
            consumer.assign(particiones);
            Map<TopicPartition, OffsetAndTimestamp> inicio = consumer.offsetsForTimes(particiones.stream()
                    .collect(Collectors.toMap(Function.identity(), tp -> desde.toEpochMilli())));
            Map<TopicPartition, Long> fin = consumer.endOffsets(particiones);
            particiones.forEach(tp -> consumer.seek(tp,
                    inicio.get(tp) != null ? inicio.get(tp).offset() : fin.get(tp)));

            int leidos = 0;
            Instant limite = Instant.now().plus(LIMITE);
            while (particiones.stream().anyMatch(tp -> consumer.position(tp) < fin.get(tp))
                    && Instant.now().isBefore(limite)) {
                for (ConsumerRecord<?, ?> r : consumer.poll(Duration.ofMillis(500))) {
                    if (r.value() instanceof PrecioEvent p && valido(p)) {
                        ventana.registrar(p.simbolo(), p.timestamp(), p.precio(), retener);
                        leidos++;
                    }
                }
            }
            log.info("Ventana reconstruida topico={} desde={} ventana={} min precios={} offsetsFin={}",
                    topico, desde, retener.toMinutes(), leidos, fin);
        }
    }

    private boolean valido(PrecioEvent p) {
        try {
            validator.validar(p);
            return true;
        } catch (PayloadInvalidoException e) {
            return false;
        }
    }
}
