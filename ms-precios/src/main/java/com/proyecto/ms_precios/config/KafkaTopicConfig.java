package com.proyecto.ms_precios.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** ms-precios es dueño de precios.actualizados: lo declara aquí y KafkaAdmin lo crea al arrancar. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic preciosTopic(AppProperties props) {
        AppProperties.Topico t = props.kafka().topics().precios();
        return TopicBuilder.name(t.nombre())
                .partitions(t.particiones())
                .replicas(t.replicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.retencionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.cleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.minInsyncReplicas()))
                .build();
    }
}
