package es.uclm.StayOn.persistence;

import es.uclm.StayOn.entity.Deseo;
import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Inquilino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeseoDAO extends JpaRepository<Deseo, Long> {

    List<Deseo> findByInquilino(Inquilino inquilino);

    Optional<Deseo> findByInquilinoAndInmueble(Inquilino inquilino, Inmueble inmueble);
}
