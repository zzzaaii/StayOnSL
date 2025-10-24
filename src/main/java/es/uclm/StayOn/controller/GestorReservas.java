package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.entity.Usuario;
import es.uclm.StayOn.persistence.ReservaDAO;

import java.util.List;

@Controller
@RequestMapping("/misReservas")
@SessionAttributes("usuario")
public class GestorReservas {

    @Autowired
    private ReservaDAO reservaDAO;

    @Autowired
    private GestorNotificaciones gestorNotificaciones; // ✅ usamos el gestor unificado

    // 🔹 Mostrar reservas del inquilino actual
    @GetMapping
    public String listarReservas(Model model, @SessionAttribute("usuario") Inquilino inquilino) {
        List<Reserva> reservas = reservaDAO.findByInquilino(inquilino);
        model.addAttribute("reservas", reservas);
        return "misReservas";
    }

    // 🔹 Mostrar reservas de los inmuebles del propietario
    @GetMapping("/propietario")
    public String listarReservasPropietario(Model model, @SessionAttribute("usuario") Propietario propietario) {
        List<Reserva> reservas = reservaDAO.findByInmueblePropietario(propietario);
        model.addAttribute("reservas", reservas);
        return "reservasPropietario";
    }

    // 🔹 Formulario para crear nueva reserva
    @GetMapping("/nueva")
    public String nuevaReserva(Model model) {
        model.addAttribute("reserva", new Reserva());
        return "formReserva";
    }

    // 🔹 Guardar una nueva reserva (notificación al propietario)
    @PostMapping("/guardar")
    public String guardarReserva(@ModelAttribute Reserva reserva, @SessionAttribute("usuario") Inquilino inquilino) {
        reserva.setInquilino(inquilino);
        reservaDAO.save(reserva);

        try {
            if (reserva.getInmueble() != null && reserva.getInmueble().getPropietario() != null) {
                gestorNotificaciones.nuevaReserva(reserva.getInmueble().getPropietario(), reserva.getInmueble());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al generar notificación de nueva reserva: " + e.getMessage());
        }

        return "redirect:/misReservas";
    }

    // 🔹 Editar reserva existente
    @GetMapping("/editar/{id}")
    public String editarReserva(@PathVariable Long id, Model model) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) {
            return "redirect:/misReservas";
        }
        model.addAttribute("reserva", reserva);
        return "formReserva";
    }

    // 🔹 Confirmar reserva (propietario)
    @GetMapping("/confirmar/{id}")
    public String confirmarReserva(@PathVariable Long id, @SessionAttribute("usuario") Propietario propietario) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) return "redirect:/misReservas/propietario";

        try {
            gestorNotificaciones.reservaConfirmada(reserva.getInquilino(), reserva.getInmueble());
        } catch (Exception e) {
            System.err.println("⚠️ Error al notificar confirmación: " + e.getMessage());
        }

        return "redirect:/misReservas/propietario";
    }

    // 🔹 Rechazar reserva (propietario)
    @GetMapping("/rechazar/{id}")
    public String rechazarReserva(@PathVariable Long id, @SessionAttribute("usuario") Propietario propietario) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) return "redirect:/misReservas/propietario";

        try {
            gestorNotificaciones.reservaRechazada(reserva.getInquilino(), reserva.getInmueble());
        } catch (Exception e) {
            System.err.println("⚠️ Error al notificar rechazo: " + e.getMessage());
        }

        reservaDAO.delete(reserva);
        return "redirect:/misReservas/propietario";
    }

    // 🔹 Eliminar / Cancelar reserva (inquilino o propietario)
    @GetMapping("/eliminar/{id}")
    public String eliminarReserva(@PathVariable Long id, @SessionAttribute("usuario") Usuario usuario) {
        Reserva reserva = reservaDAO.findById(id).orElse(null);
        if (reserva == null) {
            if (usuario instanceof Propietario) return "redirect:/misReservas/propietario";
            return "redirect:/misReservas";
        }

        try {
            if (usuario instanceof Inquilino inquilino) {
                if (reserva.getInmueble() != null && reserva.getInmueble().getPropietario() != null) {
                    gestorNotificaciones.reservaCanceladaPorInquilino(
                            reserva.getInmueble().getPropietario(), reserva.getInmueble(), inquilino);
                }
                reservaDAO.delete(reserva);
                return "redirect:/misReservas";
            } else if (usuario instanceof Propietario propietario) {
                if (reserva.getInquilino() != null) {
                    gestorNotificaciones.reservaCanceladaPorPropietario(
                            reserva.getInquilino(), reserva.getInmueble(), propietario);
                }
                reservaDAO.delete(reserva);
                return "redirect:/misReservas/propietario";
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al notificar cancelación: " + e.getMessage());
        }

        return "redirect:/misReservas";
    }
}

