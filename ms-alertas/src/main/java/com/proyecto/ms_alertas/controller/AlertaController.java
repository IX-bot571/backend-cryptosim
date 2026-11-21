package com.proyecto.ms_alertas.controller;

import com.proyecto.ms_alertas.dto.AlertaRequest;
import com.proyecto.ms_alertas.dto.AlertaResponse;
import com.proyecto.ms_alertas.entity.EstadoAlerta;
import com.proyecto.ms_alertas.service.AlertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** El usuario sale del JWT (claim sub), nunca del cuerpo. */
@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService service;

    @PostMapping
    public ResponseEntity<AlertaResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody AlertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(jwt.getSubject(), request));
    }

    @GetMapping
    public List<AlertaResponse> listar(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) EstadoAlerta estado) {
        return service.listar(jwt.getSubject(), estado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.cancelar(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
