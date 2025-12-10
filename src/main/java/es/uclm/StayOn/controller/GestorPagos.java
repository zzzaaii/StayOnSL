package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Pago;
import es.uclm.StayOn.entity.PoliticaCancelacion;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.persistence.PagoDAO;
import es.uclm.StayOn.persistence.ReservaDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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

        List<Pago> pagos = pagoDAO.findByReserva_Inquilino(inquilino);

        model.addAttribute("pagos", pagos);
        model.addAttribute("inquilino", inquilino);

        return "pagos";
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

        // Inicializar campos de reembolso
        pago.setReembolsado(false);
        pago.setImporteReembolsado(0.0);
        pago.setFechaReembolso(null);

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

    // ⭐ NUEVO: cancelar una reserva CONFIRMADA y procesar el reembolso
    @GetMapping("/cancelar/{reservaId}")
    public String cancelarReservaConfirmada(@PathVariable Long reservaId,
                                            @SessionAttribute("usuario") Inquilino inquilino) {

        Reserva reserva = reservaDAO.findById(reservaId).orElse(null);
        if (reserva == null) {
            return "redirect:/misReservas";
        }

        // Seguridad básica: que la reserva sea del inquilino logueado
        if (reserva.getInquilino() == null ||
                !reserva.getInquilino().getId().equals(inquilino.getId())) {
            return "redirect:/misReservas";
        }

        // Solo permitimos cancelar si está pagada y CONFIRMADA
        if (!reserva.isPagado() || reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            return "redirect:/misReservas";
        }

        Pago pago = reserva.getPago();
        if (pago == null) {
            System.out.println("⚠️ Reserva confirmada sin pago asociado. No se registra reembolso en Pago.");
        }

        // 1) Calcular porcentaje según la política de cancelación del inmueble
        double porcentaje = 0.0;

        if (reserva.getInmueble() != null &&
            reserva.getInmueble().getDisponibilidad() != null &&
            reserva.getInmueble().getDisponibilidad().getPoliticaCancelacion() != null) {

            PoliticaCancelacion politica =
                    reserva.getInmueble().getDisponibilidad().getPoliticaCancelacion();

            porcentaje = politica.getPorcentajeReembolso();
        }

        double total = reserva.getPrecioTotal() != null ? reserva.getPrecioTotal() : 0.0;
        double importeReembolso = total * porcentaje;

        // 2) Registrar el reembolso en el pago
        if (pago != null) {
            pago.setReembolsado(importeReembolso > 0);
            pago.setImporteReembolsado(importeReembolso);
            pago.setFechaReembolso(importeReembolso > 0 ? new Date() : null);
            pagoDAO.save(pago);
        }

        // 3) Actualizar la reserva: ya no está pagada y pasa a RECHAZADA (o CANCELADA si añades ese estado)
        reserva.setPagado(false);
        reserva.setEstado(EstadoReserva.RECHAZADA);
        reservaDAO.save(reserva);

        // 4) Notificaciones
        try {
            gestorNotificaciones.reservaCanceladaPorInquilino(
                    reserva.getInmueble().getPropietario(),
                    reserva.getInmueble(),
                    inquilino
            );
            // Aquí podrías añadir una específica tipo:
            // gestorNotificaciones.reembolsoProcesado(inquilino, reserva, importeReembolso);
        } catch (Exception e) {
            System.err.println("⚠️ Error notificando cancelación con reembolso: " + e.getMessage());
        }

        return "redirect:/misReservas";
    }
}
