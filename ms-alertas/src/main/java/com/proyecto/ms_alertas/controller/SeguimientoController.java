package com.proyecto.ms_alertas.controller;

import com.proyecto.ms_alertas.dto.SeguimientoDto;
import com.proyecto.ms_alertas.service.SeguimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seguimiento")
@RequiredArgsConstructor
public class SeguimientoController {

    private final SeguimientoService service;

    @GetMapping
    public SeguimientoDto listar(@AuthenticationPrincipal Jwt jwt) {
        return new SeguimientoDto(service.listar(jwt.getSubject()));
    }

    @PutMapping
    public SeguimientoDto reemplazar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SeguimientoDto body) {
        return new SeguimientoDto(service.reemplazar(jwt.getSubject(), body.simbolos()));
    }
}
