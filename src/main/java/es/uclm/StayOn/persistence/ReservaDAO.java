package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;
import java.util.List;

@Repository
public interface ReservaDAO extends JpaRepository<Reserva, Long> {
    List<Reserva> findByInquilino(Inquilino inquilino);

    // 🔹 Nuevo método para el panel de reservas del propietario
    List<Reserva> findByInmueblePropietario(Propietario propietario);
}
