package com.proyecto.ms_alertas.repository;

import com.proyecto.ms_alertas.entity.Alerta;
import com.proyecto.ms_alertas.entity.EstadoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findBySimboloAndEstado(String simbolo, EstadoAlerta estado);

    List<Alerta> findByUsuarioIdOrderByCreadaEnDesc(String usuarioId);

    List<Alerta> findByUsuarioIdAndEstadoOrderByCreadaEnDesc(String usuarioId, EstadoAlerta estado);

    Optional<Alerta> findByIdAndUsuarioId(Long id, String usuarioId);

    /**
     * Disparo idempotente: solo cambia la fila si sigue ACTIVA. Devuelve 1 si esta llamada la disparó y 0 si
     * ya lo había hecho otra instancia o un reintento. Se confirma en su propia transacción.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            update Alerta a set a.estado = :disparada, a.disparadaEn = :ahora, a.precioDisparo = :precio
            where a.id = :id and a.estado = :activa""")
    int marcarDisparada(@Param("id") Long id, @Param("precio") BigDecimal precio, @Param("ahora") Instant ahora,
                        @Param("activa") EstadoAlerta activa, @Param("disparada") EstadoAlerta disparada);

    /** Cancela solo si es del usuario y sigue ACTIVA (no pisa un disparo concurrente). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            update Alerta a set a.estado = :cancelada
            where a.id = :id and a.usuarioId = :usuarioId and a.estado = :activa""")
    int marcarCancelada(@Param("id") Long id, @Param("usuarioId") String usuarioId,
                        @Param("activa") EstadoAlerta activa, @Param("cancelada") EstadoAlerta cancelada);
}
