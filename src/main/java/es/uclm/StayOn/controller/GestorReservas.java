package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.persistence.ReservaDAO;

import java.util.List;

@Controller
@RequestMapping("/misReservas")
@SessionAttributes("usuario")
public class GestorReservas {

    @Autowired
    private ReservaDAO reservaDAO;

    // 🔹 Mostrar reservas del inquilino actual
    @GetMapping
    public String listarReservas(Model model, @SessionAttribute("usuario") Inquilino inquilino) {
        List<Reserva> reservas = reservaDAO.findByInquilino(inquilino);
        model.addAttribute("reservas", reservas);
        return "misReservas"; // templates/misReservas.html
    }

    // 🔹 Formulario para crear nueva reserva
    @GetMapping("/nueva")
    public String nuevaReserva(Model model) {
        model.addAttribute("reserva", new Reserva());
        return "formReserva";
    }

    // 🔹 Guardar una nueva reserva
    @PostMapping("/guardar")
    public String guardarReserva(@ModelAttribute Reserva reserva, @SessionAttribute("usuario") Inquilino inquilino) {
        reserva.setInquilino(inquilino);
        reservaDAO.save(reserva);
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

    // 🔹 Eliminar reserva
    @GetMapping("/eliminar/{id}")
    public String eliminarReserva(@PathVariable Long id) {
        reservaDAO.deleteById(id);
        return "redirect:/misReservas";
    }
}

