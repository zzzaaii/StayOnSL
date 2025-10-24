package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.Pago;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.persistence.PagoDAO;
import es.uclm.StayOn.persistence.ReservaDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/pagos")
public class GestorPagos {
	@Autowired
    private ReservaDAO reservaDAO;

    @Autowired
    private PagoDAO pagoDAO;

    // Inyectamos el gestor de notificaciones (como estaba en GestorReservas)
    @Autowired
    private GestorNotificaciones gestorNotificaciones;

    /**
     * Muestra el formulario de pago para una reserva específica.
     */
    @GetMapping("/pagar/{reservaId}")
    public String mostrarFormularioPago(@PathVariable Long reservaId, Model model) {
        Reserva reserva = reservaDAO.findById(reservaId).orElse(null);

        // No se puede pagar si la reserva no existe o ya está pagada
        if (reserva == null || reserva.isPagado()) {
            return "redirect:/misReservas";
        }

        Pago pago = new Pago();
        pago.setReserva(reserva);

        model.addAttribute("pago", pago);
        model.addAttribute("reserva", reserva);
        model.addAttribute("total", reserva.getPrecioTotal()); // Pasamos el total a la vista

        return "formularioPago"; // La nueva página HTML que crearemos
    }

    /**
     * Procesa el pago (simulado) enviado desde el formulario.
     */
    @PostMapping("/procesarPago")
    public String procesarPago(@ModelAttribute Pago pago, @RequestParam("reservaId") Long reservaId) {
        
        Reserva reserva = reservaDAO.findById(reservaId).orElse(null);
        if (reserva == null) {
            return "redirect:/misReservas"; // Error
        }

        // --- SIMULACIÓN DE PAGO ---
        // Aquí iría la lógica real para conectar con una pasarela de pago (Stripe, PayPal API).
        // Usaríamos pago.getNumeroTarjeta(), pago.getEmailPaypal(), etc.
        // Como simulación, solo generamos una referencia y guardamos.
        // --- FIN SIMULACIÓN ---

        pago.setReferencia(UUID.randomUUID().toString().substring(0, 10).toUpperCase()); // Referencia de pago única
        pago.setReserva(reserva);
        pagoDAO.save(pago); // Guardamos el objeto Pago

        reserva.setPagado(true);   // Marcamos la reserva como pagada
        reserva.setPago(pago);     // Vinculamos el pago a la reserva
        reservaDAO.save(reserva);  // Actualizamos la reserva

        // Notificar al inquilino y al propietario
        try {
            // Asumimos que gestorNotificaciones tiene estos métodos (basado en GestorReservas)
            gestorNotificaciones.pagoConfirmado(reserva.getInquilino(), reserva);
            gestorNotificaciones.pagoRecibido(reserva.getInmueble().getPropietario(), reserva);
        } catch (Exception e) {
            System.err.println("⚠️ Error al notificar pago: " + e.getMessage());
        }

        return "redirect:/misReservas"; // Volvemos a la lista de reservas
    }
  
}
