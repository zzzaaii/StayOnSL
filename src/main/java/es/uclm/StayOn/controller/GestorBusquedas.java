package es.uclm.StayOn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.entity.PoliticaCancelacion;
import es.uclm.StayOn.persistence.InmuebleDAO;
import es.uclm.StayOn.persistence.ReservaDAO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class GestorBusquedas {

    private static final Logger log = LoggerFactory.getLogger(GestorBusquedas.class);

    private static final String REDIRECT_BUSCAR_INMUEBLES = "redirect:/buscarInmuebles";
    private static final String VISTA_FORMULARIO_RESERVA = "formularioReserva";

    private static final String ATTR_INMUEBLE = "inmueble";
    private static final String ATTR_RESERVA = "reserva";
    private static final String ATTR_ERROR = "error";

    private final InmuebleDAO inmuebleDAO;
    private final ReservaDAO reservaDAO;
    private final GestorNotificaciones gestorNotificaciones;

    public GestorBusquedas(InmuebleDAO inmuebleDAO,
                           ReservaDAO reservaDAO,
                           GestorNotificaciones gestorNotificaciones) {
        this.inmuebleDAO = inmuebleDAO;
        this.reservaDAO = reservaDAO;
        this.gestorNotificaciones = gestorNotificaciones;
    }

    @GetMapping("/buscarInmuebles")
    public String mostrarPaginaBusqueda(Model model) {
        model.addAttribute("resultados", new ArrayList<Inmueble>());
        model.addAttribute("busquedaRealizada", false);
        return "busqueda";
    }

    @PostMapping("/buscar")
    public String buscarAlojamientos(
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false, defaultValue = "false") boolean directa,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) PoliticaCancelacion politica,
            Model model) {

        RangoFechas rango = parsearFechas(fechaInicio, fechaFin);

        List<Inmueble> resultados = inmuebleDAO.findAll();
        resultados = filtrarBase(resultados);
        resultados = filtrarPorDestino(resultados, destino);
        resultados = filtrarPorTipo(resultados, tipo);
        resultados = filtrarPorPrecioMax(resultados, precioMax);
        resultados = filtrarPorDirecta(resultados, directa);
        resultados = filtrarPorPolitica(resultados, politica);
        resultados = filtrarPorRangoFechas(resultados, rango);

        model.addAttribute("resultados", resultados);
        model.addAttribute("busquedaRealizada", true);
        return "busqueda";
    }

    @GetMapping("/reservar/{id}")
    public String mostrarFormularioReserva(@PathVariable Long id, Model model) {
        Inmueble inmueble = inmuebleDAO.findById(id).orElse(null);
        if (inmueble == null) return REDIRECT_BUSCAR_INMUEBLES;

        if (inmueble.isEliminado()) return REDIRECT_BUSCAR_INMUEBLES;

        model.addAttribute(ATTR_INMUEBLE, inmueble);
        model.addAttribute(ATTR_RESERVA, new Reserva());
        return VISTA_FORMULARIO_RESERVA;
    }

    @PostMapping("/confirmarReserva")
    public String confirmarReserva(@RequestParam Long inmuebleId,
                                   @ModelAttribute Reserva reserva,
                                   @SessionAttribute("usuario") Inquilino inquilino,
                                   Model model) {

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return REDIRECT_BUSCAR_INMUEBLES;

        if (inmueble.isEliminado()) return REDIRECT_BUSCAR_INMUEBLES;

        reserva.setInquilino(inquilino);
        reserva.setInmueble(inmueble);

        if (reserva.getFechaInicio() == null || reserva.getFechaFin() == null) {
            return devolverFormularioConError(model, inmueble, reserva, "Debes seleccionar fecha de inicio y fin.");
        }

        if (!reserva.getFechaInicio().before(reserva.getFechaFin())) {
            return devolverFormularioConError(model, inmueble, reserva,
                    "La fecha de fin debe ser posterior a la fecha de inicio.");
        }

        if (inmueble.getDisponibilidad() == null
                || inmueble.getDisponibilidad().getFechaInicio() == null
                || inmueble.getDisponibilidad().getFechaFin() == null) {
            return devolverFormularioConError(model, inmueble, reserva,
                    "Este inmueble no tiene un rango de disponibilidad definido.");
        }

        Date dispIni = inmueble.getDisponibilidad().getFechaInicio();
        Date dispFin = inmueble.getDisponibilidad().getFechaFin();

        if (reserva.getFechaInicio().before(dispIni) || reserva.getFechaFin().after(dispFin)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return devolverFormularioConError(model, inmueble, reserva,
                    "Esas fechas no están disponibles. Disponible solo de "
                            + sdf.format(dispIni) + " a " + sdf.format(dispFin) + ".");
        }

        boolean solapa = reservaDAO.existsSolapamiento(inmueble.getId(), reserva.getFechaInicio(), reserva.getFechaFin());
        if (solapa) {
            return devolverFormularioConError(model, inmueble, reserva,
                    "Esas fechas ya están reservadas. Elige otras fechas.");
        }

        boolean esDirecta = inmueble.getDisponibilidad() != null && inmueble.getDisponibilidad().isDirecta();

        reserva.setEstado(esDirecta ? EstadoReserva.ACEPTADA : EstadoReserva.PENDIENTE);
        reserva.setPagado(false);

        double precioAplicado = (inmueble.getPrecioPorNoche() != null) ? inmueble.getPrecioPorNoche() : 0.0;
        reserva.setPrecioPorNocheAplicado(precioAplicado);

        PoliticaCancelacion polAplicada = (inmueble.getDisponibilidad() != null)
                ? inmueble.getDisponibilidad().getPoliticaCancelacion()
                : null;
        reserva.setPoliticaCancelacionAplicada(polAplicada);

        reserva.setReservaDirectaAplicada(esDirecta);

        long noches = reserva.getNoches();
        reserva.setPrecioTotal(precioAplicado * noches);

        reservaDAO.save(reserva);

        gestorNotificaciones.enviar(inquilino, "RESERVA_REALIZADA",
                "📝 Has realizado una reserva en " + inmueble.getDireccion());
        gestorNotificaciones.enviar(inmueble.getPropietario(), "RESERVA_RECIBIDA",
                "📬 Has recibido una nueva reserva para tu inmueble: " + inmueble.getDireccion());

        model.addAttribute(ATTR_INMUEBLE, inmueble);
        model.addAttribute(ATTR_RESERVA, reserva);
        model.addAttribute("total", reserva.getPrecioTotal());
        model.addAttribute("esDirecta", esDirecta);

        return "reservaConfirmada";
    }

    private String devolverFormularioConError(Model model, Inmueble inmueble, Reserva reserva, String mensaje) {
        model.addAttribute(ATTR_ERROR, mensaje);
        model.addAttribute(ATTR_INMUEBLE, inmueble);
        model.addAttribute(ATTR_RESERVA, reserva);
        return VISTA_FORMULARIO_RESERVA;
    }

    // ==========================
    // Helpers para bajar complejidad
    // ==========================

    private static final class RangoFechas {
        final Date inicio;
        final Date fin;
        RangoFechas(Date inicio, Date fin) {
            this.inicio = inicio;
            this.fin = fin;
        }
        boolean completo() { return inicio != null && fin != null; }
    }

    private RangoFechas parsearFechas(String fechaInicio, String fechaFin) {
        Date ini = null;
        Date fin = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            if (fechaInicio != null && !fechaInicio.isEmpty()) ini = sdf.parse(fechaInicio);
            if (fechaFin != null && !fechaFin.isEmpty()) fin = sdf.parse(fechaFin);
        } catch (ParseException e) {
            log.warn("Error al parsear fechas: {}", e.getMessage());
        }

        return new RangoFechas(ini, fin);
    }

    private List<Inmueble> filtrarBase(List<Inmueble> resultados) {
        return resultados.stream()
                .filter(Objects::nonNull)
                .filter(i -> !i.isEliminado())
                .filter(i -> i.getTipo() != null || i.getCiudad() != null)
                .toList();
    }

    private List<Inmueble> filtrarPorDestino(List<Inmueble> resultados, String destino) {
        if (destino == null || destino.trim().isEmpty()) return resultados;
        String destinoLower = destino.toLowerCase();
        return resultados.stream()
                .filter(i -> (i.getCiudad() != null && i.getCiudad().toLowerCase().contains(destinoLower))
                        || (i.getDireccion() != null && i.getDireccion().toLowerCase().contains(destinoLower)))
                .toList();
    }

    private List<Inmueble> filtrarPorTipo(List<Inmueble> resultados, String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) return resultados;
        return resultados.stream()
                .filter(i -> i.getTipo() != null && i.getTipo().equalsIgnoreCase(tipo))
                .toList();
    }

    private List<Inmueble> filtrarPorPrecioMax(List<Inmueble> resultados, Double precioMax) {
        if (precioMax == null || precioMax <= 0) return resultados;
        return resultados.stream()
                .filter(i -> i.getPrecioPorNoche() != null && i.getPrecioPorNoche() <= precioMax)
                .toList();
    }

    private List<Inmueble> filtrarPorDirecta(List<Inmueble> resultados, boolean directa) {
        if (!directa) return resultados;
        return resultados.stream()
                .filter(i -> i.getDisponibilidad() != null && i.getDisponibilidad().isDirecta())
                .toList();
    }

    private List<Inmueble> filtrarPorPolitica(List<Inmueble> resultados, PoliticaCancelacion politica) {
        if (politica == null) return resultados;
        return resultados.stream()
                .filter(i -> i.getDisponibilidad() != null
                        && i.getDisponibilidad().getPoliticaCancelacion() != null
                        && i.getDisponibilidad().getPoliticaCancelacion().equals(politica))
                .toList();
    }

    private List<Inmueble> filtrarPorRangoFechas(List<Inmueble> resultados, RangoFechas rango) {
        if (rango == null || !rango.completo()) return resultados;

        Date fInicio = rango.inicio;
        Date fFin = rango.fin;

        return resultados.stream()
                .filter(i -> i.getDisponibilidad() != null
                        && i.getDisponibilidad().getFechaInicio() != null
                        && i.getDisponibilidad().getFechaFin() != null
                        && !fInicio.before(i.getDisponibilidad().getFechaInicio())
                        && !fFin.after(i.getDisponibilidad().getFechaFin()))
                .toList();
    }
}
