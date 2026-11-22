package com.proyecto.ms_alertas.regla;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import com.proyecto.ms_alertas.service.VentanaPrecios;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Optional;

/**
 * "Avísame si BTC sube 1 % en 120 minutos". Compara el precio actual contra la referencia: el primer precio
 * con timestamp ≥ ahora − ventana. variacion = (actual − referencia) / referencia × 100.
 * SUBE: variacion ≥ +p · BAJA: variacion ≤ −p · AMBAS: |variacion| ≥ p (el primer movimiento que ocurra).
 */
@Component
@RequiredArgsConstructor
public class VariacionVentanaRegla implements ReglaAlerta {

    private final VentanaPrecios ventana;

    @Override
    public TipoAlerta tipo() {
        return TipoAlerta.VARIACION_VENTANA;
    }

    @Override
    public boolean cumple(Alerta alerta, PrecioEvent precio) {
        return variacion(alerta, precio).map(v -> switch (alerta.getDireccion()) {
            case SUBE -> v.compareTo(alerta.getPorcentaje()) >= 0;
            case BAJA -> v.compareTo(alerta.getPorcentaje().negate()) <= 0;
            case AMBAS -> v.abs().compareTo(alerta.getPorcentaje()) >= 0;
        }).orElse(false);
    }

    /** Para el mensaje: indica si fue subida o bajada (importa en AMBAS) y desde qué precio. */
    @Override
    public String detalle(Alerta alerta, PrecioEvent precio) {
        return variacion(alerta, precio).map(v -> (v.signum() >= 0 ? "subió " : "bajó ")
                + v.abs().setScale(2, RoundingMode.HALF_UP).toPlainString() + " % (de "
                + referencia(alerta, precio).map(r -> r.stripTrailingZeros().toPlainString()).orElse("?")
                + " a " + precio.precio().stripTrailingZeros().toPlainString() + ")").orElse(null);
    }

    Optional<BigDecimal> variacion(Alerta alerta, PrecioEvent precio) {
        return referencia(alerta, precio).map(ref -> precio.precio().subtract(ref)
                .multiply(BigDecimal.valueOf(100))
                .divide(ref, 8, RoundingMode.HALF_UP));
    }

    private Optional<BigDecimal> referencia(Alerta alerta, PrecioEvent precio) {
        return ventana.referencia(alerta.getSimbolo(),
                precio.timestamp().minus(Duration.ofMinutes(alerta.getVentanaMinutos())));
    }
}
