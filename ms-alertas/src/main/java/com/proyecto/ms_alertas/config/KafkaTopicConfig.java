package com.proyecto.ms_alertas.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** ms-alertas es dueño de alertas.disparadas: lo declara aquí y KafkaAdmin lo crea al arrancar. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic alertasDisparadasTopic(AppProperties props) {
        AppProperties.Topico t = props.kafka().topics().alertasDisparadas();
        return TopicBuilder.name(t.nombre())
                .partitions(t.particiones())
                .replicas(t.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retencionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()))
                .build();
    }
}
