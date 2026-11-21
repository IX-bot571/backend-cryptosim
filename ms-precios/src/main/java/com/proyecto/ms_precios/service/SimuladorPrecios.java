package com.proyecto.ms_precios.service;

import com.proyecto.ms_precios.config.AppProperties;
import com.proyecto.ms_precios.config.AppProperties.Cripto;
import com.proyecto.ms_precios.dto.PrecioEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Caminata aleatoria: precio(t+1) = precio(t) * (1 + z * volatilidad%), con z ~ N(0,1).
 * Si antes hubo precios reales, continúa desde el último precio de Binance (ver {@link #sincronizar}).
 */
@Component
public class SimuladorPrecios {

    private static final int ESCALA_PRECIO = 8;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final BigDecimal volatilidadPct;
    private final Map<String, Estado> estados = new HashMap<>();

    private static final class Estado {
        BigDecimal precio, apertura, maximo, minimo, volumen;

        Estado(BigDecimal precio, BigDecimal apertura, BigDecimal maximo, BigDecimal minimo, BigDecimal volumen) {
            this.precio = precio;
            this.apertura = apertura;
            this.maximo = maximo;
            this.minimo = minimo;
            this.volumen = volumen;
        }
    }

    public SimuladorPrecios(AppProperties props) {
        this.volatilidadPct = props.precios().volatilidadPct();
    }

    /** Toma el último precio real como punto de partida de la simulación. */
    public synchronized void sincronizar(PrecioEvent real) {
        BigDecimal apertura = real.precio().divide(
                BigDecimal.ONE.add(real.variacion24hPct().divide(CIEN, MathContext.DECIMAL64)), MathContext.DECIMAL64);
        estados.put(real.simbolo(), new Estado(real.precio(), apertura, real.maximo24h(), real.minimo24h(), real.volumen24h()));
    }

    public synchronized PrecioEvent siguiente(Cripto cripto, Instant ahora) {
        Estado e = estados.computeIfAbsent(cripto.simbolo(), s -> {
            BigDecimal base = cripto.precioBase().setScale(ESCALA_PRECIO, RoundingMode.HALF_UP);
            return new Estado(base, base, base, base, BigDecimal.ZERO);
        });
        BigDecimal z = BigDecimal.valueOf(ThreadLocalRandom.current().nextGaussian());
        BigDecimal factor = BigDecimal.ONE.add(volatilidadPct.multiply(z).divide(CIEN, MathContext.DECIMAL64));
        BigDecimal nuevo = e.precio.multiply(factor).setScale(ESCALA_PRECIO, RoundingMode.HALF_UP);
        if (nuevo.signum() > 0) {
            e.precio = nuevo;
        }
        e.maximo = e.maximo.max(e.precio);
        e.minimo = e.minimo.min(e.precio);
        BigDecimal variacion = e.precio.subtract(e.apertura)
                .multiply(CIEN)
                .divide(e.apertura, 3, RoundingMode.HALF_UP);
        return new PrecioEvent(cripto.simbolo(), e.precio, variacion, e.maximo, e.minimo, e.volumen,
                ahora, PrecioEvent.FUENTE_SIMULADO);
    }
}
