package com.proyecto.ms_admin_kafka.service;

import com.proyecto.ms_admin_kafka.dto.CrearTopicRequest;
import com.proyecto.ms_admin_kafka.dto.GrupoDetalle;
import com.proyecto.ms_admin_kafka.dto.GrupoResumen;
import com.proyecto.ms_admin_kafka.dto.ParticionInfo;
import com.proyecto.ms_admin_kafka.dto.TopicDetalle;
import com.proyecto.ms_admin_kafka.dto.TopicResumen;
import com.proyecto.ms_admin_kafka.exception.KafkaNoDisponibleException;
import com.proyecto.ms_admin_kafka.exception.OperacionNoPermitidaException;
import com.proyecto.ms_admin_kafka.exception.RecursoNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Operaciones de administración sobre tópicos y consumer groups usando el AdminClient de Kafka. */
@Slf4j
@Service
public class KafkaAdminService {

    private static final Comparator<TopicPartition> POR_TOPICO_Y_PARTICION =
            Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition);

    private final AdminClient admin;
    private final long timeoutSegundos;

    public KafkaAdminService(AdminClient admin, @Value("${app.admin.timeout-segundos}") long timeoutSegundos) {
        this.admin = admin;
        this.timeoutSegundos = timeoutSegundos;
    }

    // ---------------------------------------------------------------- Tópicos

    public List<TopicResumen> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics().names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        Map<String, Map<String, String>> configs = configsExplicitas(nombres);
        return nombres.stream().sorted()
                .map(n -> resumen(descripciones.get(n), configs.get(n)))
                .toList();
    }

    public TopicDetalle detalleTopico(String nombre) {
        TopicDescription d = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        TopicResumen r = resumen(d, configsExplicitas(List.of(nombre)).get(nombre));
        List<ParticionInfo> particiones = d.partitions().stream()
                .map(p -> new ParticionInfo(p.partition(), p.leader() == null ? null : p.leader().id(),
                        ids(p.replicas()), ids(p.isr())))
                .toList();
        return new TopicDetalle(r.nombre(), r.particiones(), r.replicas(), r.configs(), particiones);
    }

    public TopicResumen crearTopico(CrearTopicRequest req) {
        Map<String, String> configs = req.configs() == null ? Map.of() : req.configs();
        NewTopic topic = new NewTopic(req.name(), req.particiones(), req.replicas().shortValue()).configs(configs);
        esperar(admin.createTopics(List.of(topic)).all());
        log.info("Tópico creado nombre={} particiones={} replicas={} configs={}",
                req.name(), req.particiones(), req.replicas(), configs);
        return new TopicResumen(req.name(), req.particiones(), req.replicas(), new TreeMap<>(configs));
    }

    /** incrementalAlterConfigs: SET para cada valor; un valor null hace DELETE (vuelve al valor por defecto). */
    public TopicDetalle cambiarConfig(String nombre, Map<String, String> cambios) {
        exigirTopico(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        List<AlterConfigOp> ops = cambios.entrySet().stream()
                .map(e -> e.getValue() == null
                        ? new AlterConfigOp(new ConfigEntry(e.getKey(), null), AlterConfigOp.OpType.DELETE)
                        : new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, ops)).all());
        log.info("Configs modificadas topico={} cambios={}", nombre, cambios);
        return detalleTopico(nombre);
    }

    /** Kafka solo permite AUMENTAR particiones (las claves existentes pueden cambiar de partición). */
    public TopicDetalle aumentarParticiones(String nombre, int total) {
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.info("Particiones aumentadas topico={} total={}", nombre, total);
        // La metadata de las particiones nuevas tarda unos milisegundos en propagarse a todos los brokers:
        // se espera brevemente para no devolver el número antiguo.
        TopicDetalle detalle = detalleTopico(nombre);
        for (int i = 0; i < 10 && detalle.particiones() < total; i++) {
            pausa(200);
            detalle = detalleTopico(nombre);
        }
        return detalle;
    }

    public void eliminarTopico(String nombre) {
        if (nombre.startsWith("__")) {
            throw new OperacionNoPermitidaException("No se pueden eliminar tópicos internos: " + nombre);
        }
        exigirTopico(nombre);
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("Tópico eliminado nombre={}", nombre);
    }

    // ---------------------------------------------------------------- Consumer groups

    public List<GrupoResumen> listarGrupos() {
        return esperar(admin.listGroups(ListGroupsOptions.forConsumerGroups()).all()).stream()
                .map(g -> new GrupoResumen(g.groupId(),
                        g.groupState().map(Enum::name).orElse("DESCONOCIDO"),
                        g.type().map(Enum::name).orElse("-")))
                .sorted(Comparator.comparing(GrupoResumen::groupId))
                .toList();
    }

    /** Miembros con sus particiones asignadas, offsets comprometidos y lag = fin del log − offset comprometido. */
    public GrupoDetalle detalleGrupo(String groupId) {
        ConsumerGroupDescription g = esperar(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        Map<TopicPartition, OffsetAndMetadata> comprometidos =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        if (g.groupState() == GroupState.DEAD && g.members().isEmpty() && comprometidos.isEmpty()) {
            throw new RecursoNoEncontradoException("Consumer group no encontrado: " + groupId);
        }

        List<GrupoDetalle.Miembro> miembros = g.members().stream()
                .map(m -> new GrupoDetalle.Miembro(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream().sorted(POR_TOPICO_Y_PARTICION)
                                .map(TopicPartition::toString).toList()))
                .sorted(Comparator.comparing(GrupoDetalle.Miembro::consumerId))
                .toList();

        // Particiones a mostrar: las que tienen offset comprometido y las asignadas a algún miembro.
        Set<TopicPartition> particiones = new TreeSet<>(POR_TOPICO_Y_PARTICION);
        particiones.addAll(comprometidos.keySet());
        g.members().forEach(m -> particiones.addAll(m.assignment().topicPartitions()));

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales = particiones.isEmpty() ? Map.of()
                : esperar(admin.listOffsets(particiones.stream()
                        .collect(Collectors.toMap(Function.identity(), tp -> OffsetSpec.latest()))).all());

        List<GrupoDetalle.OffsetParticion> offsets = new ArrayList<>();
        long lagTotal = 0;
        for (TopicPartition tp : particiones) {
            long fin = finales.get(tp).offset();
            OffsetAndMetadata om = comprometidos.get(tp);
            Long comprometido = om == null ? null : om.offset();
            Long lag = comprometido == null ? null : Math.max(0, fin - comprometido);
            if (lag != null) {
                lagTotal += lag;
            }
            offsets.add(new GrupoDetalle.OffsetParticion(tp.topic(), tp.partition(), comprometido, fin, lag));
        }
        Node c = g.coordinator();
        return new GrupoDetalle(g.groupId(), g.groupState().name(),
                c == null ? null : c.id() + " (" + c.host() + ":" + c.port() + ")", miembros, offsets, lagTotal);
    }

    // ---------------------------------------------------------------- Auxiliares

    private void exigirTopico(String nombre) {
        esperar(admin.describeTopics(List.of(nombre)).allTopicNames());
    }

    private TopicResumen resumen(TopicDescription d, Map<String, String> configs) {
        int replicas = d.partitions().isEmpty() ? 0 : d.partitions().getFirst().replicas().size();
        return new TopicResumen(d.name(), d.partitions().size(), replicas, configs == null ? Map.of() : configs);
    }

    /** Solo las configs definidas en el propio tópico (DYNAMIC_TOPIC_CONFIG), ordenadas por nombre. */
    private Map<String, Map<String, String>> configsExplicitas(Collection<String> nombres) {
        List<ConfigResource> recursos = nombres.stream()
                .map(n -> new ConfigResource(ConfigResource.Type.TOPIC, n)).toList();
        Map<ConfigResource, Config> todas = esperar(admin.describeConfigs(recursos).all());
        Map<String, Map<String, String>> resultado = new TreeMap<>();
        todas.forEach((recurso, config) -> resultado.put(recurso.name(), config.entries().stream()
                .filter(e -> e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value, (a, b) -> b, TreeMap::new))));
        return resultado;
    }

    private static void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }

    /**
     * Espera el resultado del AdminClient con timeout. Desenvuelve ExecutionException para que el
     * GlobalExceptionHandler reciba la excepción real de Kafka (TopicExistsException, UnknownTopic...).
     */
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutSegundos, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new KafkaNoDisponibleException("Error de Kafka: " + e.getCause(), e.getCause());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondió en " + timeoutSegundos + " s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida", e);
        }
    }
}
