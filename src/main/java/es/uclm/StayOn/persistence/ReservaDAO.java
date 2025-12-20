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

    List<Reserva> findByInquilino(Inquilino inquilino);

    // Panel reservas propietario
    List<Reserva> findByInmueblePropietario(Propietario propietario);

    // comprueba si existe solape (reservas NO rechazadas)
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

    // existe reserva activa (fechaFin >= hoy) y no rechazada
    @Query("""
        SELECT COUNT(r) > 0
        FROM Reserva r
        WHERE r.inmueble.id = :inmuebleId
          AND r.estado <> es.uclm.StayOn.entity.Reserva$EstadoReserva.RECHAZADA
          AND r.fechaFin >= :hoy
    """)
    boolean existsReservaActiva(@Param("inmuebleId") Long inmuebleId,
                                @Param("hoy") Date hoy);

    // (EDITAR): si hay reserva activa que quedaría fuera del nuevo rango
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
