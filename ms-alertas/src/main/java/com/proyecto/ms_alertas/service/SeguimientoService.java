package com.proyecto.ms_alertas.service;

import com.proyecto.ms_alertas.entity.Seguimiento;
import com.proyecto.ms_alertas.repository.SeguimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeguimientoService {

    private final SeguimientoRepository repository;
    private final CatalogoService catalogo;

    public List<String> listar(String usuarioId) {
        return repository.findByUsuarioIdOrderBySimboloAsc(usuarioId).stream().map(Seguimiento::getSimbolo).toList();
    }

    /** Reemplaza la lista completa. Valida todo antes de borrar: si un símbolo es inválido no cambia nada. */
    @Transactional
    public List<String> reemplazar(String usuarioId, List<String> simbolos) {
        Set<String> validos = new LinkedHashSet<>();
        simbolos.forEach(s -> validos.add(catalogo.validar(s)));
        repository.borrarDeUsuario(usuarioId);
        repository.saveAll(validos.stream().map(s -> new Seguimiento(usuarioId, s)).toList());
        return validos.stream().sorted().toList();
    }
}
