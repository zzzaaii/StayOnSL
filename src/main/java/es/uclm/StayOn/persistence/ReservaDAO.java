package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservaDAO extends JpaRepository<Reserva, Long> {

    // ✅ Solo las NO ocultas para el inquilino
    @Query("""
        SELECT r
        FROM Reserva r
        WHERE r.inquilino = :inquilino
          AND r.ocultaParaInquilino = false
    """)
    List<Reserva> findByInquilino(@Param("inquilino") Inquilino inquilino);

    // Panel reservas propietario (sin cambios)
    List<Reserva> findByInmueblePropietario(Propietario propietario);

    @Query("""
        SELECT COUNT(r) > 0
        FROM Reserva r
        WHERE r.inmueble.id = :inmuebleId
          AND r.estado <> es.uclm.StayOn.entity.Reserva$EstadoReserva.RECHAZADA
          AND r.fechaInicio < :fin
          AND r.fechaFin > :inicio
    """)
    boolean existsSolapamiento(@Param("inmuebleId") Long inmuebleId,
                              @Param("inicio") Date inicio,
                              @Param("fin") Date fin);

    @Query("""
        SELECT COUNT(r) > 0
        FROM Reserva r
        WHERE r.inmueble.id = :inmuebleId
          AND r.estado <> es.uclm.StayOn.entity.Reserva$EstadoReserva.RECHAZADA
          AND r.fechaFin >= :hoy
    """)
    boolean existsReservaActiva(@Param("inmuebleId") Long inmuebleId,
                                @Param("hoy") Date hoy);

    @Query("""
        SELECT COUNT(r) > 0
        FROM Reserva r
        WHERE r.inmueble.id = :inmuebleId
          AND r.estado <> es.uclm.StayOn.entity.Reserva$EstadoReserva.RECHAZADA
          AND r.fechaFin >= :hoy
          AND (r.fechaInicio < :nuevoInicio OR r.fechaFin > :nuevoFin)
    """)
    boolean existsReservaActivaFueraDeRango(@Param("inmuebleId") Long inmuebleId,
                                           @Param("hoy") Date hoy,
                                           @Param("nuevoInicio") Date nuevoInicio,
                                           @Param("nuevoFin") Date nuevoFin);
}
