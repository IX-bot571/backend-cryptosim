package com.proyecto.ms_precios.service;

import com.proyecto.ms_precios.client.BinanceClient;
import com.proyecto.ms_precios.config.AppProperties;
import com.proyecto.ms_precios.config.AppProperties.Cripto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Catálogo de criptos activas. Al arrancar comprueba cada par contra Binance: el que no existe se registra
 * en el log y se omite. Si Binance no responde, se usa el catálogo completo y se reintenta en el siguiente ciclo.
 */
@Slf4j
@Service
public class CatalogoService {

    private final AppProperties props;
    private final BinanceClient binance;
    private volatile List<Cripto> activos;
    private volatile boolean validado;

    public CatalogoService(AppProperties props, BinanceClient binance) {
        this.props = props;
        this.binance = binance;
        this.activos = List.copyOf(props.catalogo());
        this.validado = props.precios().modo() == AppProperties.Modo.SIMULADO;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validarAlArrancar() {
        validarSiHaceFalta();
    }

    public synchronized void validarSiHaceFalta() {
        if (validado) {
            return;
        }
        try {
            List<Cripto> validos = new ArrayList<>();
            for (Cripto c : props.catalogo()) {
                if (binance.parExiste(c.par())) {
                    validos.add(c);
                } else {
                    log.warn("Par {} ({}) no existe en Binance: se omite del catálogo", c.par(), c.simbolo());
                }
            }
            activos = List.copyOf(validos);
            validado = true;
            log.info("Catálogo validado contra Binance: {} criptos activas {}", activos.size(),
                    activos.stream().map(Cripto::simbolo).toList());
        } catch (RestClientException e) {
            log.warn("No se pudo validar el catálogo contra Binance ({}); se reintentará en el próximo ciclo",
                    e.getMessage());
        }
    }

    public List<Cripto> activos() {
        return activos;
    }

    public Optional<Cripto> buscar(String simbolo) {
        return activos.stream().filter(c -> c.simbolo().equalsIgnoreCase(simbolo)).findFirst();
    }
}
