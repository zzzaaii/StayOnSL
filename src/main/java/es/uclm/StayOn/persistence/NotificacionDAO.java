package es.uclm.StayOn.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import es.uclm.StayOn.entity.Notificacion;
import es.uclm.StayOn.entity.Usuario;
import java.util.List;

@Repository
public interface NotificacionDAO extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioDestinoOrderByFechaDesc(Usuario usuario);
}
