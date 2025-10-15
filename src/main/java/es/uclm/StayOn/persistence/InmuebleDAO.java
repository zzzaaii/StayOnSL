package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Propietario;
import java.util.List;

@Repository
public interface InmuebleDAO extends JpaRepository<Inmueble, Long> {
    List<Inmueble> findByPropietario(Propietario propietario);
}
