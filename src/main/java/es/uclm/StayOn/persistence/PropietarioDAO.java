package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import es.uclm.StayOn.entity.Propietario;

@Repository
public interface PropietarioDAO extends JpaRepository<Propietario, Long> { }
