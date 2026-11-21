package com.proyecto.ms_precios.controller;

import com.proyecto.ms_precios.config.AppProperties.Cripto;
import com.proyecto.ms_precios.dto.CriptoResponse;
import com.proyecto.ms_precios.dto.PrecioEvent;
import com.proyecto.ms_precios.dto.PuntoHistorial;
import com.proyecto.ms_precios.exception.SimboloNoEncontradoException;
import com.proyecto.ms_precios.service.CatalogoService;
import com.proyecto.ms_precios.service.HistorialPrecios;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PreciosController {

    private final CatalogoService catalogo;
    private final HistorialPrecios historial;

    @GetMapping("/catalogo")
    public List<CriptoResponse> catalogo() {
        return catalogo.activos().stream().map(c -> new CriptoResponse(c.simbolo(), c.nombre())).toList();
    }

    @GetMapping("/precios")
    public List<PrecioEvent> precios() {
        return historial.ultimos(catalogo.activos().stream().map(Cripto::simbolo).toList());
    }

    @GetMapping("/precios/{simbolo}/historial")
    public List<PuntoHistorial> historial(@PathVariable String simbolo,
                                          @RequestParam(defaultValue = "60") @Min(1) @Max(1440) int minutos) {
        Cripto cripto = catalogo.buscar(simbolo).orElseThrow(() -> new SimboloNoEncontradoException(simbolo));
        return historial.historial(cripto.simbolo(), minutos);
    }
}
