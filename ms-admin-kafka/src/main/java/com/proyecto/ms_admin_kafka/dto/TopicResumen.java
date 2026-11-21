package com.proyecto.ms_admin_kafka.dto;

import java.util.Map;

/** configs: solo las definidas explícitamente en el tópico (no los valores por defecto del broker). */
public record TopicResumen(String nombre, int particiones, int replicas, Map<String, String> configs) {}
