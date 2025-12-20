package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

@Controller
public class GestorBusquedas {

    @Autowired
    private InmuebleDAO inmuebleDAO;

    @Autowired
    private ReservaDAO reservaDAO;

    @Autowired
    private GestorNotificaciones gestorNotificaciones;

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

        Date fechaInicioDate = null;
        Date fechaFinDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            if (fechaInicio != null && !fechaInicio.isEmpty()) fechaInicioDate = sdf.parse(fechaInicio);
            if (fechaFin != null && !fechaFin.isEmpty()) fechaFinDate = sdf.parse(fechaFin);
        } catch (ParseException e) {
            System.out.println("⚠️ Error al parsear fechas: " + e.getMessage());
        }

        List<Inmueble> resultados = inmuebleDAO.findAll();

        // no mostrar inmuebles eliminados (borrado lógico)
        resultados = resultados.stream()
                .filter(Objects::nonNull)
                .filter(i -> !i.isEliminado())
                .filter(i -> i.getTipo() != null || i.getCiudad() != null)
                .collect(Collectors.toList());

        // Filtro por destino (ciudad o dirección)
        if (destino != null && !destino.trim().isEmpty()) {
            String destinoLower = destino.toLowerCase();
            resultados = resultados.stream()
                    .filter(i -> (i.getCiudad() != null && i.getCiudad().toLowerCase().contains(destinoLower))
                            || (i.getDireccion() != null && i.getDireccion().toLowerCase().contains(destinoLower)))
                    .collect(Collectors.toList());
        }

        // Filtro por tipo
        if (tipo != null && !tipo.trim().isEmpty()) {
            resultados = resultados.stream()
                    .filter(i -> i.getTipo() != null && i.getTipo().equalsIgnoreCase(tipo))
                    .collect(Collectors.toList());
        }

        // Filtro por precio máximo
        if (precioMax != null && precioMax > 0) {
            resultados = resultados.stream()
                    .filter(i -> i.getPrecioPorNoche() != null && i.getPrecioPorNoche() <= precioMax)
                    .collect(Collectors.toList());
        }

        // Filtro por reserva inmediata (directa)
        if (directa) {
            resultados = resultados.stream()
                    .filter(i -> i.getDisponibilidad() != null && i.getDisponibilidad().isDirecta())
                    .collect(Collectors.toList());
        }

        // Filtro por política de cancelación
        if (politica != null) {
            final PoliticaCancelacion politicaSeleccionada = politica;
            resultados = resultados.stream()
                    .filter(i -> i.getDisponibilidad() != null
                            && i.getDisponibilidad().getPoliticaCancelacion() != null
                            && i.getDisponibilidad().getPoliticaCancelacion().equals(politicaSeleccionada))
                    .collect(Collectors.toList());
        }

        // Filtro por rango de fechas (dentro del rango disponible)
        final Date fInicio = fechaInicioDate;
        final Date fFin = fechaFinDate;
        if (fInicio != null && fFin != null) {
            resultados = resultados.stream()
                    .filter(i -> i.getDisponibilidad() != null
                            && i.getDisponibilidad().getFechaInicio() != null
                            && i.getDisponibilidad().getFechaFin() != null
                            && !fInicio.before(i.getDisponibilidad().getFechaInicio())
                            && !fFin.after(i.getDisponibilidad().getFechaFin()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("resultados", resultados);
        model.addAttribute("busquedaRealizada", true);
        return "busqueda";
    }

    @GetMapping("/reservar/{id}")
    public String mostrarFormularioReserva(@PathVariable Long id, Model model) {
        Inmueble inmueble = inmuebleDAO.findById(id).orElse(null);
        if (inmueble == null) return "redirect:/buscarInmuebles";

        // si está eliminado, no se puede reservar
        if (inmueble.isEliminado()) return "redirect:/buscarInmuebles";

        model.addAttribute("inmueble", inmueble);
        model.addAttribute("reserva", new Reserva());
        return "formularioReserva";
    }

    @PostMapping("/confirmarReserva")
    public String confirmarReserva(@RequestParam Long inmuebleId,
                                   @ModelAttribute Reserva reserva,
                                   @SessionAttribute("usuario") Inquilino inquilino,
                                   Model model) {

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return "redirect:/buscarInmuebles";

        // si está eliminado, no se puede reservar
        if (inmueble.isEliminado()) return "redirect:/buscarInmuebles";

        reserva.setInquilino(inquilino);
        reserva.setInmueble(inmueble);

        // VALIDACIÓN 0: fechas obligatorias
        if (reserva.getFechaInicio() == null || reserva.getFechaFin() == null) {
            model.addAttribute("error", "Debes seleccionar fecha de inicio y fin.");
            model.addAttribute("inmueble", inmueble);
            model.addAttribute("reserva", reserva);
            return "formularioReserva";
        }

        // VALIDACIÓN 1: inicio < fin
        if (!reserva.getFechaInicio().before(reserva.getFechaFin())) {
            model.addAttribute("error", "La fecha de fin debe ser posterior a la fecha de inicio.");
            model.addAttribute("inmueble", inmueble);
            model.addAttribute("reserva", reserva);
            return "formularioReserva";
        }

        // VALIDACIÓN 2: dentro del rango disponible del propietario
        if (inmueble.getDisponibilidad() == null
                || inmueble.getDisponibilidad().getFechaInicio() == null
                || inmueble.getDisponibilidad().getFechaFin() == null) {
            model.addAttribute("error", "Este inmueble no tiene un rango de disponibilidad definido.");
            model.addAttribute("inmueble", inmueble);
            model.addAttribute("reserva", reserva);
            return "formularioReserva";
        }

        Date dispIni = inmueble.getDisponibilidad().getFechaInicio();
        Date dispFin = inmueble.getDisponibilidad().getFechaFin();

        if (reserva.getFechaInicio().before(dispIni) || reserva.getFechaFin().after(dispFin)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            model.addAttribute("error",
                    "Esas fechas no están disponibles. Disponible solo de "
                            + sdf.format(dispIni) + " a " + sdf.format(dispFin) + ".");
            model.addAttribute("inmueble", inmueble);
            model.addAttribute("reserva", reserva);
            return "formularioReserva";
        }

        // VALIDACIÓN 3: sin solape con reservas existentes del mismo inmueble (no rechazadas)
        boolean solapa = reservaDAO.existsSolapamiento(inmueble.getId(), reserva.getFechaInicio(), reserva.getFechaFin());
        if (solapa) {
            model.addAttribute("error", "Esas fechas ya están reservadas. Elige otras fechas.");
            model.addAttribute("inmueble", inmueble);
            model.addAttribute("reserva", reserva);
            return "formularioReserva";
        }

        // DIRECTA / PENDIENTE (lo “congelamos” en la reserva)
        boolean esDirecta = inmueble.getDisponibilidad() != null && inmueble.getDisponibilidad().isDirecta();

        if (esDirecta) {
            reserva.setEstado(EstadoReserva.ACEPTADA);
            reserva.setPagado(false);
        } else {
            reserva.setEstado(EstadoReserva.PENDIENTE);
            reserva.setPagado(false);
        }

        
        double precioAplicado = (inmueble.getPrecioPorNoche() != null) ? inmueble.getPrecioPorNoche() : 0.0;
        reserva.setPrecioPorNocheAplicado(precioAplicado);

        PoliticaCancelacion polAplicada = null;
        if (inmueble.getDisponibilidad() != null) {
            polAplicada = inmueble.getDisponibilidad().getPoliticaCancelacion();
        }
        reserva.setPoliticaCancelacionAplicada(polAplicada);

        reserva.setReservaDirectaAplicada(esDirecta);

        long noches = reserva.getNoches();
        reserva.setPrecioTotal(precioAplicado * noches);
      

        reservaDAO.save(reserva);

        gestorNotificaciones.enviar(inquilino, "RESERVA_REALIZADA",
                "📝 Has realizado una reserva en " + inmueble.getDireccion());
        gestorNotificaciones.enviar(inmueble.getPropietario(), "RESERVA_RECIBIDA",
                "📬 Has recibido una nueva reserva para tu inmueble: " + inmueble.getDireccion());

        model.addAttribute("inmueble", inmueble);
        model.addAttribute("reserva", reserva);
        model.addAttribute("total", reserva.getPrecioTotal()); 
        model.addAttribute("esDirecta", esDirecta);

        return "reservaConfirmada";
    }
}
