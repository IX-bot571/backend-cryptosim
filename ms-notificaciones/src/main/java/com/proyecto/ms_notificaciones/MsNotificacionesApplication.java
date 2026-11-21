package com.proyecto.ms_notificaciones;

import com.proyecto.ms_notificaciones.config.KafkaNames;
import com.proyecto.ms_notificaciones.config.RabbitNames;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RabbitNames.class, KafkaNames.class})
public class MsNotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNotificacionesApplication.class, args);
    }
}
