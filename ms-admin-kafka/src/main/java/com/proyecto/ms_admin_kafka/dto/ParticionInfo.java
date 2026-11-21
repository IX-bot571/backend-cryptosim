package com.proyecto.ms_admin_kafka.dto;

import java.util.List;

public record ParticionInfo(int particion, Integer lider, List<Integer> replicas, List<Integer> isr) {}
