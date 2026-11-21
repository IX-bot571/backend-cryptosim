package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.config.AppProperties;
import com.proyecto.ms_alertas.exception.SimboloNoValidoException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CatalogoService {

    private final Set<String> simbolos;

    public CatalogoService(AppProperties props) {
        this.simbolos = props.catalogo().stream().map(AppProperties.Cripto::simbolo).collect(Collectors.toUnmodifiableSet());
    }

    public boolean contiene(String simbolo) {
        return simbolo != null && simbolos.contains(simbolo);
    }

    /** Normaliza a mayúsculas y comprueba que esté en el catálogo. */
    public String validar(String simbolo) {
        String s = simbolo == null ? "" : simbolo.trim().toUpperCase(Locale.ROOT);
        if (!simbolos.contains(s)) {
            throw new SimboloNoValidoException(simbolo);
        }
        return s;
    }
}
