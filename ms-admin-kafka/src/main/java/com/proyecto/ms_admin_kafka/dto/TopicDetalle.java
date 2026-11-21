package com.proyecto.ms_admin_kafka.dto;

import java.util.List;
import java.util.Map;

public record TopicDetalle(String nombre, int particiones, int replicas, Map<String, String> configs,
                           List<ParticionInfo> detalleParticiones) {}
