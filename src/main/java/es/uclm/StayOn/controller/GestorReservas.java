package es.uclm.StayOn.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.entity.Usuario;
import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.persistence.ReservaDAO;

import java.util.Date;

@Controller
@RequestMapping("/misReservas")
public class GestorReservas {

    private static final Logger logger = LoggerFactory.getLogger(GestorReservas.class);

    private static final String REDIRECT_MIS_RESERVAS = "redirect:/misReservas";
    private static final String REDIRECT_MIS_RESERVAS_PROP = "redirect:/misReservas/propietario";

    private final ReservaDAO reservaDAO;
    private final GestorNotificaciones gestorNotificaciones;

    public GestorReservas(ReservaDAO reservaDAO, GestorNotificaciones gestorNotificaciones) {
        this.reservaDAO = reservaDAO;
        this.gestorNotificaciones = gestorNotificaciones;
    }

    @GetMapping
    public String listarReservas(Model model, @SessionAttribute("usuario") Inquilino inquilino) {
        model.addAttribute("reservas", reservaDAO.findByInquilino(inquilino));
        return "misReservas";
    }

    @GetMapping("/propietario")
    public String listarReservasPropietario(Model model, @SessionAttribute("usuario") Propietario propietario) {
        model.addAttribute("reservas", reservaDAO.findByInmueblePropietario(propietario));
        return "reservasPropietario";
    }

    @GetMapping("/nueva")
    public String nuevaReserva(Model model) {
        model.addAttribute("reserva", new Reserva());
        return "formReserva";
    }

    /**
     * Intencionadamente siempre volvemos al listado (éxito o error).
     * Los tests exigen exactamente este redirect.
     */
    @SuppressWarnings("java:S3516")
    @PostMapping("/guardar")
    public String guardarReserva(@ModelAttribute Reserva reserva,
                                 @SessionAttribute("usuario") Inquilino inquilino) {

        reserva.setInquilino(inquilino);
        Inmueble inmueble = reserva.getInmueble();

        if (!tieneDatosMinimos(reserva, inmueble)) {
            return REDIRECT_MIS_RESERVAS;
        }

        if (!rangoFechasValido(reserva.getFechaInicio(), reserva.getFechaFin())) {
            return REDIRECT_MIS_RESERVAS;
        }

        if (!disponibilidadCompleta(inmueble)) {
            return REDIRECT_MIS_RESERVAS;
        }

        if (!dentroDeDisponibilidad(reserva, inmueble)) {
            return REDIRECT_MIS_RESERVAS;
        }

        if (reservaDAO.existsSolapamiento(
                inmueble.getId(),
                reserva.getFechaInicio(),
                reserva.getFechaFin())) {
            return REDIRECT_MIS_RESERVAS;
        }

        reserva.setEstado(
                inmueble.getDisponibilidad().isDirecta()
                        ? EstadoReserva.ACEPTADA
                        : EstadoReserva.PENDIENTE
        );

        reservaDAO.save(reserva);
        notificarNuevaReservaSiProcede(inmueble);

        return REDIRECT_MIS_RESERVAS;
    }

    @SuppressWarnings("java:S3516")
    @GetMapping("/aceptar/{id}")
    public String aceptarReserva(@PathVariable Long id,
                                 @SessionAttribute("usuario") Propietario propietario) {

        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva != null) {
            reserva.setEstado(EstadoReserva.ACEPTADA);
            reservaDAO.save(reserva);
            notificarAceptacion(reserva);
        }

        return REDIRECT_MIS_RESERVAS_PROP;
    }

    @SuppressWarnings("java:S3516")
    @GetMapping("/rechazar/{id}")
    public String rechazarReserva(@PathVariable Long id,
                                  @SessionAttribute("usuario") Propietario propietario) {

        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva != null) {
            reserva.setEstado(EstadoReserva.RECHAZADA);
            procesarDevolucion(reserva);
            reservaDAO.save(reserva);
            notificarRechazo(reserva);
        }

        return REDIRECT_MIS_RESERVAS_PROP;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarReserva(@PathVariable Long id,
                                  @SessionAttribute("usuario") Usuario usuario) {

        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) {
            return (usuario instanceof Propietario)
                    ? REDIRECT_MIS_RESERVAS_PROP
                    : REDIRECT_MIS_RESERVAS;
        }

        try {
            if (usuario instanceof Inquilino inquilino) {

                // Seguridad: que sea su reserva
                if (reserva.getInquilino() == null
                        || reserva.getInquilino().getId() == null
                        || !reserva.getInquilino().getId().equals(inquilino.getId())) {
                    return REDIRECT_MIS_RESERVAS;
                }

               
                boolean tienePago = (reserva.getPago() != null) || reserva.isPagado();
                if (tienePago) {
                    reserva.setOcultaParaInquilino(true);
                    reservaDAO.save(reserva);
                    return REDIRECT_MIS_RESERVAS;
                }

                // Si NO hay pago, se puede borrar (como antes)
                gestorNotificaciones.reservaCanceladaPorInquilino(
                        reserva.getInmueble().getPropietario(),
                        reserva.getInmueble(),
                        inquilino
                );
                reservaDAO.delete(reserva);
                return REDIRECT_MIS_RESERVAS;
            }

            if (usuario instanceof Propietario propietario) {
                gestorNotificaciones.reservaCanceladaPorPropietario(
                        reserva.getInquilino(),
                        reserva.getInmueble(),
                        propietario
                );
                reservaDAO.delete(reserva);
                return REDIRECT_MIS_RESERVAS_PROP;
            }

        } catch (Exception e) {
            logger.error("Error al notificar cancelación de reserva", e);
        }

        return REDIRECT_MIS_RESERVAS;
    }


    // ----------------- Helpers -----------------

    private boolean tieneDatosMinimos(Reserva reserva, Inmueble inmueble) {
        return inmueble != null
                && inmueble.getId() != null
                && reserva.getFechaInicio() != null
                && reserva.getFechaFin() != null;
    }

    private boolean rangoFechasValido(Date inicio, Date fin) {
        return inicio.before(fin);
    }

    private boolean disponibilidadCompleta(Inmueble inmueble) {
        return inmueble.getDisponibilidad() != null
                && inmueble.getDisponibilidad().getFechaInicio() != null
                && inmueble.getDisponibilidad().getFechaFin() != null;
    }

    private boolean dentroDeDisponibilidad(Reserva reserva, Inmueble inmueble) {
        Date dispIni = inmueble.getDisponibilidad().getFechaInicio();
        Date dispFin = inmueble.getDisponibilidad().getFechaFin();
        return !reserva.getFechaInicio().before(dispIni)
                && !reserva.getFechaFin().after(dispFin);
    }

    private void notificarNuevaReservaSiProcede(Inmueble inmueble) {
        try {
            if (inmueble.getPropietario() != null) {
                gestorNotificaciones.nuevaReserva(
                        inmueble.getPropietario(),
                        inmueble
                );
            }
        } catch (Exception e) {
            logger.error("Error al notificar nueva reserva", e);
        }
    }

    private void notificarAceptacion(Reserva reserva) {
        try {
            gestorNotificaciones.reservaConfirmada(
                    reserva.getInquilino(),
                    reserva.getInmueble()
            );
        } catch (Exception e) {
            logger.error("Error al notificar aceptación de reserva", e);
        }
    }

    private void notificarRechazo(Reserva reserva) {
        try {
            gestorNotificaciones.reservaRechazada(
                    reserva.getInquilino(),
                    reserva.getInmueble()
            );
        } catch (Exception e) {
            logger.error("Error al notificar rechazo de reserva", e);
        }
    }

    private void procesarDevolucion(Reserva reserva) {
        if (reserva == null) return;
        reserva.setPagado(false);
        logger.info(
                "Dinero devuelto al inquilino por reserva {}",
                reserva.getId()
        );
    }
}
