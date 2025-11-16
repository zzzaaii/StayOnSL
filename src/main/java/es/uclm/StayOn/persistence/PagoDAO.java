package es.uclm.StayOn.persistence;

import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoDAO extends JpaRepository<Pago, Long> {

    // Historial de pagos de un inquilino (vía reserva.inquilino)
    List<Pago> findByReserva_Inquilino(Inquilino inquilino);
}
