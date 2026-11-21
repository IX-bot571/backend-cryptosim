package com.proyecto.ms_admin_kafka.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce los errores de Kafka a respuestas HTTP claras: 404 no existe, 409 ya existe, 400 inválido, 503 caído. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errores.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos", "detalle", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> ilegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Cuerpo JSON inválido"));
    }

    @ExceptionHandler({UnknownTopicOrPartitionException.class, GroupIdNotFoundException.class,
            RecursoNoEncontradoException.class})
    ResponseEntity<Map<String, Object>> noEncontrado(RuntimeException e) {
        return respuesta(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(TopicExistsException.class)
    ResponseEntity<Map<String, Object>> yaExiste(TopicExistsException e) {
        return respuesta(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler({InvalidPartitionsException.class, InvalidReplicationFactorException.class,
            InvalidConfigurationException.class, InvalidTopicException.class, InvalidRequestException.class,
            PolicyViolationException.class, OperacionNoPermitidaException.class})
    ResponseEntity<Map<String, Object>> invalido(RuntimeException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler({TimeoutException.class, KafkaNoDisponibleException.class})
    ResponseEntity<Map<String, Object>> noDisponible(RuntimeException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    @ExceptionHandler(KafkaException.class)
    ResponseEntity<Map<String, Object>> kafka(KafkaException e) {
        log.error("Error de Kafka no clasificado", e);
        return respuesta(HttpStatus.BAD_GATEWAY, e);
    }

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, RuntimeException e) {
        return ResponseEntity.status(status).body(Map.of("error", String.valueOf(e.getMessage()),
                "tipo", e.getClass().getSimpleName()));
    }
}
