package com.proyecto.ms_precios.client;

import com.proyecto.ms_precios.config.AppProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Cliente de la API pública de Binance (sin API key). Los errores de red se propagan como RestClientException. */
@Component
public class BinanceClient {

    private final RestClient restClient;

    public BinanceClient(AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.binance().connectTimeout());
        factory.setReadTimeout(props.binance().readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(props.binance().url())
                .requestFactory(factory)
                .build();
    }

    /** Un solo request para todos los pares: symbols=["BTCUSDT","ETHUSDT",...]. */
    public List<BinanceTicker> tickers24h(Collection<String> pares) {
        String symbols = pares.stream().map(p -> "\"" + p + "\"").collect(Collectors.joining(",", "[", "]"));
        BinanceTicker[] respuesta = restClient.get()
                .uri(u -> u.path("/api/v3/ticker/24hr").queryParam("symbols", "{s}").build(symbols))
                .retrieve()
                .body(BinanceTicker[].class);
        return respuesta == null ? List.of() : List.of(respuesta);
    }

    /** true si el par existe; Binance responde 400 si el símbolo no es válido. */
    public boolean parExiste(String par) {
        try {
            restClient.get()
                    .uri(u -> u.path("/api/v3/exchangeInfo").queryParam("symbol", "{s}").build(par))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        }
    }
}
