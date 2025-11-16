package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import es.uclm.StayOn.entity.Disponibilidad;
import es.uclm.StayOn.entity.Inmueble;
import java.util.List;

@Repository
public interface DisponibilidadDAO extends JpaRepository<Disponibilidad, Long> {
    List<Disponibilidad> findByInmueble(Inmueble inmueble);
}
