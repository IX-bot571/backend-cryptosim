package com.proyecto.ms_alertas;

import com.proyecto.ms_alertas.config.AppProperties;
import com.proyecto.ms_alertas.config.RabbitNames;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RabbitNames.class, AppProperties.class})
public class MsAlertasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAlertasApplication.class, args);
    }
}
