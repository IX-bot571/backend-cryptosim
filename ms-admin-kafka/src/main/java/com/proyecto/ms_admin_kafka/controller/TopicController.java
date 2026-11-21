package com.proyecto.ms_admin_kafka.controller;

import com.proyecto.ms_admin_kafka.dto.ConfigRequest;
import com.proyecto.ms_admin_kafka.dto.CrearTopicRequest;
import com.proyecto.ms_admin_kafka.dto.ParticionesRequest;
import com.proyecto.ms_admin_kafka.dto.TopicDetalle;
import com.proyecto.ms_admin_kafka.dto.TopicResumen;
import com.proyecto.ms_admin_kafka.service.KafkaAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final KafkaAdminService service;

    @GetMapping
    public List<TopicResumen> listar() {
        return service.listarTopicos();
    }

    @GetMapping("/{name}")
    public TopicDetalle detalle(@PathVariable String name) {
        return service.detalleTopico(name);
    }

    @PostMapping
    public ResponseEntity<TopicResumen> crear(@Valid @RequestBody CrearTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTopico(request));
    }

    @PutMapping("/{name}/config")
    public TopicDetalle cambiarConfig(@PathVariable String name, @Valid @RequestBody ConfigRequest request) {
        return service.cambiarConfig(name, request.configs());
    }

    @PutMapping("/{name}/partitions")
    public TopicDetalle aumentarParticiones(@PathVariable String name, @Valid @RequestBody ParticionesRequest request) {
        return service.aumentarParticiones(name, request.particiones());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> eliminar(@PathVariable String name) {
        service.eliminarTopico(name);
        return ResponseEntity.noContent().build();
    }
}
