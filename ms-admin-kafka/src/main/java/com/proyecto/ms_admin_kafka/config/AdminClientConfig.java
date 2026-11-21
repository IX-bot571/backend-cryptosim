package com.proyecto.ms_admin_kafka.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Un único AdminClient para todo el servicio. Toma bootstrap y timeouts de spring.kafka.* a través del
 * KafkaAdmin que configura Boot. Spring lo cierra al apagar (destroyMethod = close).
 */
@Configuration
public class AdminClientConfig {

    @Bean(destroyMethod = "close")
    AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
}
