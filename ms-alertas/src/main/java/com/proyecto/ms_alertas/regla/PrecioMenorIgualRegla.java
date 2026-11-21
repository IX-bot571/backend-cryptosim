package com.proyecto.ms_alertas.regla;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;
import org.springframework.stereotype.Component;

@Component
public class PrecioMenorIgualRegla implements ReglaAlerta {

    @Override
    public TipoAlerta tipo() {
        return TipoAlerta.PRECIO_MENOR_IGUAL;
    }

    @Override
    public boolean cumple(Alerta alerta, PrecioEvent precio) {
        return precio.precio().compareTo(alerta.getValor()) <= 0;
    }
}
