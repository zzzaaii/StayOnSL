package es.uclm.StayOn.persistence;
import es.uclm.StayOn.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoDAO extends JpaRepository<Pago, Long> {

}
