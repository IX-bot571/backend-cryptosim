package com.proyecto.ms_admin_kafka.controller;

import com.proyecto.ms_admin_kafka.dto.GrupoDetalle;
import com.proyecto.ms_admin_kafka.dto.GrupoResumen;
import com.proyecto.ms_admin_kafka.service.KafkaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/consumer-groups")
@RequiredArgsConstructor
public class ConsumerGroupController {

    private final KafkaAdminService service;

    @GetMapping
    public List<GrupoResumen> listar() {
        return service.listarGrupos();
    }

    @GetMapping("/{id}")
    public GrupoDetalle detalle(@PathVariable String id) {
        return service.detalleGrupo(id);
    }
}
