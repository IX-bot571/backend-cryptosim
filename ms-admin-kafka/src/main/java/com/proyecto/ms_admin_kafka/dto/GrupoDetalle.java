package com.proyecto.ms_admin_kafka.dto;

import java.util.List;

public record GrupoDetalle(String groupId, String estado, String coordinador, List<Miembro> miembros,
                           List<OffsetParticion> offsets, long lagTotal) {

    public record Miembro(String consumerId, String clientId, String host, List<String> particiones) {}

    /** comprometido/lag son null si el grupo aún no confirmó offsets en esa partición. */
    public record OffsetParticion(String topico, int particion, Long comprometido, long finLog, Long lag) {}
}
