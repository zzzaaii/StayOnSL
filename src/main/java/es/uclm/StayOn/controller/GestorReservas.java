package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

@Controller
@RequestMapping("/misReservas")
@SessionAttributes("usuario")
public class GestorReservas {

    @Autowired
    private ReservaDAO reservaDAO;

    @Autowired
    private GestorNotificaciones gestorNotificaciones; // gestor unificado

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

    @PostMapping("/guardar")
    public String guardarReserva(@ModelAttribute Reserva reserva, @SessionAttribute("usuario") Inquilino inquilino) {
        reserva.setInquilino(inquilino);
        Inmueble inmueble = reserva.getInmueble();

        if (inmueble != null && inmueble.getDisponibilidad() != null && inmueble.getDisponibilidad().isDirecta()) {
            reserva.setEstado(EstadoReserva.ACEPTADA);
        } else {
            reserva.setEstado(EstadoReserva.PENDIENTE);
        }

        reservaDAO.save(reserva);

        try {
            if (inmueble != null && inmueble.getPropietario() != null) {
                gestorNotificaciones.nuevaReserva(inmueble.getPropietario(), inmueble);
            }
        } catch (Exception e) {
            System.err.println("Error al notificar nueva reserva: " + e.getMessage());
        }

        return "redirect:/misReservas";
    }

    @GetMapping("/aceptar/{id}")
    public String aceptarReserva(@PathVariable Long id, @SessionAttribute("usuario") Propietario propietario) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) return "redirect:/misReservas/propietario";

        reserva.setEstado(EstadoReserva.ACEPTADA);
        reservaDAO.save(reserva);

        try {
            gestorNotificaciones.reservaConfirmada(reserva.getInquilino(), reserva.getInmueble());
        } catch (Exception e) {
            System.err.println("Error al notificar aceptación: " + e.getMessage());
        }

        return "redirect:/misReservas/propietario";
    }

    @GetMapping("/rechazar/{id}")
    public String rechazarReserva(@PathVariable Long id, @SessionAttribute("usuario") Propietario propietario) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) return "redirect:/misReservas/propietario";

        reserva.setEstado(EstadoReserva.RECHAZADA);
        procesarDevolucion(reserva);
        reservaDAO.save(reserva);

        try {
            gestorNotificaciones.reservaRechazada(reserva.getInquilino(), reserva.getInmueble());
        } catch (Exception e) {
            System.err.println("Error al notificar rechazo: " + e.getMessage());
        }

        return "redirect:/misReservas/propietario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarReserva(@PathVariable Long id, @SessionAttribute("usuario") Usuario usuario) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) {
            return (usuario instanceof Propietario) ? "redirect:/misReservas/propietario" : "redirect:/misReservas";
        }

        try {
            if (usuario instanceof Inquilino inquilino) {
                gestorNotificaciones.reservaCanceladaPorInquilino(
                        reserva.getInmueble().getPropietario(),
                        reserva.getInmueble(),
                        inquilino
                );
                reservaDAO.delete(reserva);
                return "redirect:/misReservas";
            }

            if (usuario instanceof Propietario propietario) {
                gestorNotificaciones.reservaCanceladaPorPropietario(
                        reserva.getInquilino(),
                        reserva.getInmueble(),
                        propietario
                );
                reservaDAO.delete(reserva);
                return "redirect:/misReservas/propietario";
            }

        } catch (Exception e) {
            System.err.println("Error al notificar cancelación: " + e.getMessage());
        }

        return "redirect:/misReservas";
    }

    private void procesarDevolucion(Reserva reserva) {
        if (reserva == null) return;
        reserva.setPagado(false);
        System.out.println("Dinero devuelto al inquilino por reserva " + reserva.getId());
    }
}
