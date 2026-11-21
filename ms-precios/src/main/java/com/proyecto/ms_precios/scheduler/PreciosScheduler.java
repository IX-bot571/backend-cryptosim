package com.proyecto.ms_precios.scheduler;

import com.proyecto.ms_precios.dto.PrecioEvent;
import com.proyecto.ms_precios.producer.PrecioPublisher;
import com.proyecto.ms_precios.service.HistorialPrecios;
import com.proyecto.ms_precios.service.ObtencionPreciosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cada app.precios.intervalo-ms: obtiene precios, los guarda en el historial y publica un evento por cripto. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreciosScheduler {

    private final ObtencionPreciosService obtencion;
    private final HistorialPrecios historial;
    private final PrecioPublisher publisher;

    @Scheduled(fixedRateString = "${app.precios.intervalo-ms}")
    public void ciclo() {
        List<PrecioEvent> precios = obtencion.obtener();
        precios.forEach(p -> {
            historial.registrar(p);
            publisher.publicar(p);
        });
        log.info("Ciclo de precios: {} eventos enviados (fuente={})", precios.size(),
                precios.isEmpty() ? "-" : precios.getFirst().fuente());
    }
}
