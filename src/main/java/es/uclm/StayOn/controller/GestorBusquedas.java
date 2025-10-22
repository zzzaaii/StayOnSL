package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Reserva;
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


    // 🔹 Página principal de búsqueda
    @GetMapping("/buscarInmuebles")
    public String mostrarPaginaBusqueda(Model model) {
        model.addAttribute("resultados", new ArrayList<Inmueble>());
        model.addAttribute("busquedaRealizada", false);
        return "busqueda";
    }

    // 🔹 Procesar filtros de búsqueda
    @PostMapping("/buscar")
    public String buscarAlojamientos(
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false, defaultValue = "false") boolean directa,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            Model model) {

        Date fechaInicioDate = null;
        Date fechaFinDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                fechaInicioDate = sdf.parse(fechaInicio);
            }
            if (fechaFin != null && !fechaFin.isEmpty()) {
                fechaFinDate = sdf.parse(fechaFin);
            }
        } catch (ParseException e) {
            System.out.println("⚠️ Error al parsear fechas: " + e.getMessage());
        }

        List<Inmueble> resultados = inmuebleDAO.findAll();

        // 🔹 Filtrar nulos
        resultados = resultados.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getTipo() != null || i.getCiudad() != null)
                .collect(Collectors.toList());

        // 🔹 Filtro ciudad/dirección
        if (destino != null && !destino.trim().isEmpty()) {
            String destinoLower = destino.toLowerCase();
            resultados = resultados.stream()
                    .filter(i -> (i.getCiudad() != null && i.getCiudad().toLowerCase().contains(destinoLower))
                            || (i.getDireccion() != null && i.getDireccion().toLowerCase().contains(destinoLower)))
                    .collect(Collectors.toList());
        }

        // 🔹 Filtro tipo
        if (tipo != null && !tipo.trim().isEmpty()) {
            resultados = resultados.stream()
                    .filter(i -> i.getTipo() != null && i.getTipo().equalsIgnoreCase(tipo))
                    .collect(Collectors.toList());
        }

        // 🔹 Filtro precio máximo
        if (precioMax != null && precioMax > 0) {
            resultados = resultados.stream()
                    .filter(i -> i.getPrecioPorNoche() != null && i.getPrecioPorNoche() <= precioMax)
                    .collect(Collectors.toList());
        }

        // 🔹 Filtro reserva inmediata
        if (directa) {
            resultados = resultados.stream()
                    .filter(i -> i.getDisponibilidad() != null && i.getDisponibilidad().isDirecta())
                    .collect(Collectors.toList());
        }

        // 🔹 Filtro fechas
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

    // 🔹 Mostrar formulario de reserva
    @GetMapping("/reservar/{id}")
    public String mostrarFormularioReserva(@PathVariable Long id, Model model) {
        Inmueble inmueble = inmuebleDAO.findById(id).orElse(null);
        if (inmueble == null) {
            return "redirect:/buscarInmuebles";
        }

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
if (inmueble == null) {
return "redirect:/buscarInmuebles";
}

// 🔹 Asociar inquilino e inmueble
reserva.setInquilino(inquilino);
reserva.setInmueble(inmueble);

// 🔹 Guardar reserva
reservaDAO.save(reserva);

// 🔹 Mostrar mensaje de confirmación
model.addAttribute("inmueble", inmueble);
model.addAttribute("reserva", reserva);
model.addAttribute("total", reserva.getPrecioTotal());

return "reservaConfirmada"; // crea esta vista si aún no existe
}


}

