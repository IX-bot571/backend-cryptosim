package com.proyecto.ms_alertas.repository;

import com.proyecto.ms_alertas.entity.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {

    List<Seguimiento> findByUsuarioIdOrderBySimboloAsc(String usuarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Seguimiento s where s.usuarioId = :usuarioId")
    void borrarDeUsuario(@Param("usuarioId") String usuarioId);
}
