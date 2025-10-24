package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import es.uclm.StayOn.persistence.NotificacionDAO;
import es.uclm.StayOn.entity.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/notificaciones")
public class GestorNotificaciones {

    @Autowired
    private NotificacionDAO notificacionDAO;

    // ====================================================
    // 🔹 SECCIÓN 1: MOSTRAR Y GESTIONAR NOTIFICACIONES
    // ====================================================

    @GetMapping
    public String verNotificaciones(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        List<Notificacion> notificaciones = notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(usuario);
        model.addAttribute("notificaciones", notificaciones);
        model.addAttribute("esInquilino", usuario instanceof Inquilino);
        model.addAttribute("esPropietario", usuario instanceof Propietario);

        return "notificaciones";
    }

    @GetMapping("/leida/{id}")
    public String marcarComoLeida(@PathVariable Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        Notificacion notificacion = notificacionDAO.findById(id).orElse(null);
        if (notificacion != null && notificacion.getUsuarioDestino().getId().equals(usuario.getId())) {
            notificacion.setLeido(true);
            notificacionDAO.save(notificacion);
        }

        return "redirect:/notificaciones";
    }

    @GetMapping("/limpiar")
    public String limpiarLeidas(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) return "redirect:/login";

        List<Notificacion> notificaciones = notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(usuario);
        notificaciones.stream().filter(Notificacion::isLeido).forEach(notificacionDAO::delete);
        return "redirect:/notificaciones";
    }

    // ====================================================
    // 🔹 SECCIÓN 2: MÉTODOS PARA CREAR NOTIFICACIONES
    // ====================================================

    /** Método genérico para enviar cualquier tipo de notificación */
    public void enviar(Usuario destino, String tipo, String mensaje) {
        if (destino == null || mensaje == null || mensaje.isBlank()) return;

        Notificacion n = new Notificacion();
        n.setUsuarioDestino(destino);
        n.setTipo(tipo);
        n.setMensaje(mensaje);
        n.setFecha(new Date());
        n.setLeido(false);
        notificacionDAO.save(n);
    }

    // ===============================
    // 🔸 EVENTOS DE RESERVAS
    // ===============================

    public void nuevaReserva(Propietario propietario, Inmueble inmueble) {
        enviar(propietario, "RESERVA_NUEVA",
                "📩 Nueva solicitud de reserva para tu inmueble: " + inmueble.getDireccion());
    }

    public void reservaConfirmada(Inquilino inquilino, Inmueble inmueble) {
        enviar(inquilino, "RESERVA_CONFIRMADA",
                "✅ Tu reserva en " + inmueble.getDireccion() + " ha sido confirmada por el propietario.");
    }

    public void reservaRechazada(Inquilino inquilino, Inmueble inmueble) {
        enviar(inquilino, "RESERVA_RECHAZADA",
                "❌ Tu solicitud de reserva en " + inmueble.getDireccion() + " ha sido rechazada.");
    }

    public void reservaCanceladaPorInquilino(Propietario propietario, Inmueble inmueble, Inquilino inquilino) {
        enviar(propietario, "RESERVA_CANCELADA_INQUILINO",
                "⚠️ El inquilino " + inquilino.getNombre() + " ha cancelado la reserva en " + inmueble.getDireccion());
    }

    public void reservaCanceladaPorPropietario(Inquilino inquilino, Inmueble inmueble, Propietario propietario) {
        enviar(inquilino, "RESERVA_CANCELADA_PROPIETARIO",
                "⚠️ El propietario " + propietario.getNombre() + " ha cancelado tu reserva en " + inmueble.getDireccion());
    }

    public void reservaProxima(Usuario usuario, Inmueble inmueble) {
        enviar(usuario, "RESERVA_PROXIMA",
                "🕓 Tu reserva en " + inmueble.getDireccion() + " comienza mañana.");
    }

    // ===============================
    // 🔸 EVENTOS DE INMUEBLES
    // ===============================

    public void inmueblePublicado(Propietario propietario, Inmueble inmueble) {
        enviar(propietario, "INMUEBLE_PUBLICADO",
                "🏡 Tu inmueble '" + inmueble.getDireccion() + "' se ha publicado correctamente en StayOn.");
    }

    public void inmuebleActualizado(Propietario propietario, Inmueble inmueble) {
        enviar(propietario, "INMUEBLE_ACTUALIZADO",
                "✏️ Has actualizado la información de tu inmueble: " + inmueble.getDireccion());
    }

    // ===============================
    // 🔸 EVENTOS DE PAGOS
    // ===============================

    public void pagoConfirmado(Inquilino inquilino, Reserva reserva) {
        enviar(inquilino, "PAGO_CONFIRMADO",
                "💳 Tu pago de la reserva en " + reserva.getDireccion() + " ha sido procesado con éxito.");
    }

    public void pagoRecibido(Propietario propietario, Reserva reserva) {
        enviar(propietario, "PAGO_RECIBIDO",
                "💰 Has recibido el pago de la reserva en " + reserva.getDireccion() + ".");
    }
 // ====================================================
 // 🔹 SECCIÓN 3: ENDPOINT PARA CONTADOR AJAX
 // ====================================================
 @GetMapping("/noLeidas")
 @ResponseBody
 public long contarNoLeidas(HttpSession session) {
     Usuario usuario = (Usuario) session.getAttribute("usuario");
     if (usuario == null) return 0;
     return notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(usuario)
             .stream()
             .filter(n -> !n.isLeido())
             .count();
 }

}
