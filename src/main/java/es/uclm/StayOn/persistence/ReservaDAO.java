package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Inquilino;
import java.util.List;

@Repository
public interface ReservaDAO extends JpaRepository<Reserva, Long> {
    List<Reserva> findByInquilino(Inquilino inquilino);
}
