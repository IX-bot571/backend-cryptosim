package com.proyecto.ms_precios.service;

import com.proyecto.ms_precios.client.BinanceClient;
import com.proyecto.ms_precios.client.BinanceTicker;
import com.proyecto.ms_precios.config.AppProperties;
import com.proyecto.ms_precios.config.AppProperties.Cripto;
import com.proyecto.ms_precios.config.AppProperties.Modo;
import com.proyecto.ms_precios.dto.PrecioEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Obtiene un precio por cripto según app.precios.modo. No sabe nada de Kafka. */
@Slf4j
@Service
public class ObtencionPreciosService {

    private final Modo modo;
    private final CatalogoService catalogo;
    private final BinanceClient binance;
    private final SimuladorPrecios simulador;
    private volatile boolean enRespaldo;

    public ObtencionPreciosService(AppProperties props, CatalogoService catalogo,
                                   BinanceClient binance, SimuladorPrecios simulador) {
        this.modo = props.precios().modo();
        this.catalogo = catalogo;
        this.binance = binance;
        this.simulador = simulador;
        log.info("Modo de precios: {}", modo);
    }

    public List<PrecioEvent> obtener() {
        Instant ahora = Instant.now();
        return switch (modo) {
            case SIMULADO -> simulados(ahora);
            case BINANCE -> {
                try {
                    yield desdeBinance(ahora);
                } catch (RestClientException e) {
                    log.error("Binance no responde y el modo es BINANCE (sin respaldo): ciclo omitido. Causa: {}",
                            e.getMessage());
                    yield List.of();
                }
            }
            case BINANCE_CON_RESPALDO -> {
                try {
                    List<PrecioEvent> reales = desdeBinance(ahora);
                    if (enRespaldo) {
                        enRespaldo = false;
                        log.info("Binance responde otra vez: se vuelve a fuente=BINANCE");
                    }
                    yield reales;
                } catch (RestClientException e) {
                    if (!enRespaldo) {
                        enRespaldo = true;
                        log.warn("Binance no responde ({}): se pasa al simulador (fuente=SIMULADO)", e.getMessage());
                    }
                    yield simulados(ahora);
                }
            }
        };
    }

    private List<PrecioEvent> desdeBinance(Instant ahora) {
        catalogo.validarSiHaceFalta();
        List<Cripto> activos = catalogo.activos();
        Map<String, BinanceTicker> porPar = binance.tickers24h(activos.stream().map(Cripto::par).toList())
                .stream().collect(Collectors.toMap(BinanceTicker::symbol, Function.identity()));
        List<PrecioEvent> eventos = new ArrayList<>(activos.size());
        for (Cripto c : activos) {
            BinanceTicker t = porPar.get(c.par());
            if (t == null) {
                log.warn("Binance no devolvió ticker para {} ({})", c.par(), c.simbolo());
                continue;
            }
            PrecioEvent evento = new PrecioEvent(c.simbolo(), t.lastPrice(), t.priceChangePercent(),
                    t.highPrice(), t.lowPrice(), t.volume(), ahora, PrecioEvent.FUENTE_BINANCE);
            simulador.sincronizar(evento);
            eventos.add(evento);
        }
        return eventos;
    }

    private List<PrecioEvent> simulados(Instant ahora) {
        return catalogo.activos().stream().map(c -> simulador.siguiente(c, ahora)).toList();
    }
}
