package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Pago;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.persistence.PagoDAO;
import es.uclm.StayOn.persistence.ReservaDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/pagos")
public class GestorPagos {

    @Autowired
    private ReservaDAO reservaDAO;

    @Autowired
    private PagoDAO pagoDAO;

    @Autowired
    private GestorNotificaciones gestorNotificaciones;

    // ⭐ Pantalla principal: HISTORIAL DE PAGOS
    @GetMapping
    public String verPagos(@SessionAttribute("usuario") Inquilino inquilino,
                           Model model) {

        // Obtener historial del usuario
        List<Pago> pagos = pagoDAO.findByReserva_Inquilino(inquilino);

        model.addAttribute("pagos", pagos);
        model.addAttribute("inquilino", inquilino);

        return "pagos";   // ⇦ este será el HTML del historial
    }

    // ⭐ Mostrar formulario de pago para una reserva concreta
    @GetMapping("/pagar/{reservaId}")
    public String mostrarFormularioPago(@PathVariable Long reservaId, Model model) {

        Reserva reserva = reservaDAO.findById(reservaId).orElse(null);

        if (reserva == null || reserva.isPagado() || reserva.getEstado() != EstadoReserva.ACEPTADA) {
            return "redirect:/misReservas";
        }

        Pago pago = new Pago();
        pago.setReserva(reserva);

        model.addAttribute("pago", pago);
        model.addAttribute("reserva", reserva);
        model.addAttribute("total", reserva.getPrecioTotal());

        return "formularioPago";
    }

    // ⭐ Procesar el pago de la reserva
    @PostMapping("/procesarPago")
    public String procesarPago(@ModelAttribute Pago pago,
                               @RequestParam("reservaId") Long reservaId) {

        Reserva reserva = reservaDAO.findById(reservaId).orElse(null);

        if (reserva == null) {
            return "redirect:/misReservas";
        }

        // Generar referencia aleatoria
        pago.setReferencia(UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        pago.setReserva(reserva);
        pagoDAO.save(pago);

        reserva.setPagado(true);
        reserva.setPago(pago);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaDAO.save(reserva);

        try {
            gestorNotificaciones.pagoConfirmado(reserva.getInquilino(), reserva);
            gestorNotificaciones.pagoRecibido(reserva.getInmueble().getPropietario(), reserva);
        } catch (Exception e) {
            System.err.println("⚠️ Error enviando notificaciones: " + e.getMessage());
        }

        return "redirect:/misReservas";
    }
}

