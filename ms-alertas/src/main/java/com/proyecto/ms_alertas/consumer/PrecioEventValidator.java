package com.proyecto.ms_alertas.consumer;

import com.proyecto.ms_alertas.dto.PrecioEvent;
import com.proyecto.ms_alertas.exception.PayloadInvalidoException;
import com.proyecto.ms_alertas.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reglas mínimas del payload: simbolo del catálogo, precio > 0 y timestamp presente. */
@Component
@RequiredArgsConstructor
public class PrecioEventValidator {

    private final CatalogoService catalogo;

    public void validar(PrecioEvent p) {
        if (p == null) {
            throw new PayloadInvalidoException("Evento vacío");
        }
        if (p.simbolo() == null || p.simbolo().isBlank() || !catalogo.contiene(p.simbolo())) {
            throw new PayloadInvalidoException("Símbolo vacío o fuera del catálogo: " + p.simbolo());
        }
        if (p.precio() == null || p.precio().signum() <= 0) {
            throw new PayloadInvalidoException("Precio debe ser > 0: " + p.precio());
        }
        if (p.timestamp() == null) {
            throw new PayloadInvalidoException("Falta timestamp");
        }
    }
}
