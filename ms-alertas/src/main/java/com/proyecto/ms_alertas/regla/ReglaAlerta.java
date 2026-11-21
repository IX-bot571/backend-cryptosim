package com.proyecto.ms_alertas.regla;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.TipoAlerta;

/** Patrón estrategia: una implementación por tipo de alerta. Agregar un tipo = agregar una clase. */
public interface ReglaAlerta {

    TipoAlerta tipo();

    boolean cumple(Alerta alerta, PrecioEvent precio);
}
