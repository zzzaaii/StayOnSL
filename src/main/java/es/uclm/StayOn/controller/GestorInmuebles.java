package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Disponibilidad;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.persistence.InmuebleDAO;
import es.uclm.StayOn.persistence.DisponibilidadDAO;
import es.uclm.StayOn.persistence.ReservaDAO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gestionInmuebles")
@SessionAttributes("usuario")
public class GestorInmuebles {

    @Autowired
    private InmuebleDAO inmuebleDAO;

    @Autowired
    private DisponibilidadDAO disponibilidadDAO;

    @Autowired
    private ReservaDAO reservaDAO;

    @Autowired
    private GestorNotificaciones gestorNotificaciones;

    @GetMapping
    public String listarInmuebles(Model model, @SessionAttribute("usuario") Propietario propietario) {
        List<Inmueble> inmuebles = inmuebleDAO.findByPropietario(propietario)
                .stream()
                .filter(i -> i != null && !i.isEliminado())
                .collect(Collectors.toList());

        model.addAttribute("inmuebles", inmuebles);
        return "gestionInmuebles";
    }

    @GetMapping("/nuevo")
    public String nuevoInmueble(Model model) {
        Inmueble inmueble = new Inmueble();
        inmueble.setDisponibilidad(new Disponibilidad());
        model.addAttribute("inmueble", inmueble);
        return "forminmueble";
    }

    @GetMapping("/editar/{id}")
    public String editarInmueble(@PathVariable Long id, Model model, @SessionAttribute("usuario") Propietario propietario) {
        Optional<Inmueble> optionalInmueble = inmuebleDAO.findById(id);
        if (optionalInmueble.isEmpty()
                || !optionalInmueble.get().getPropietario().getId().equals(propietario.getId())
                || optionalInmueble.get().isEliminado()) {
            return "redirect:/gestionInmuebles";
        }

        Inmueble inmueble = optionalInmueble.get();
        if (inmueble.getDisponibilidad() == null) {
            inmueble.setDisponibilidad(new Disponibilidad());
        }

        model.addAttribute("inmueble", inmueble);
        return "forminmueble";
    }

    @PostMapping("/guardar")
    public String guardarInmueble(@ModelAttribute Inmueble inmueble,
                                  @SessionAttribute("usuario") Propietario propietario,
                                  Model model) {

        boolean esNuevo = (inmueble.getId() == null);

        if (inmueble.getTipo() == null || inmueble.getTipo().isBlank()) inmueble.setTipo("Vivienda");
        if (inmueble.getDireccion() == null || inmueble.getDireccion().isBlank()) inmueble.setDireccion("Sin dirección");
        if (inmueble.getCiudad() == null || inmueble.getCiudad().isBlank()) inmueble.setCiudad("Sin ciudad");
        if (inmueble.getPrecioPorNoche() == null) inmueble.setPrecioPorNoche(0.1);

        inmueble.setPropietario(propietario);

        // Asegurar disponibilidad no null
        if (inmueble.getDisponibilidad() == null) {
            inmueble.setDisponibilidad(new Disponibilidad());
        }

      
        if (!esNuevo) {
            Inmueble original = inmuebleDAO.findById(inmueble.getId()).orElse(null);
            if (original == null) return "redirect:/gestionInmuebles";

            // Seguridad: solo dueño y no eliminado
            if (!original.getPropietario().getId().equals(propietario.getId()) || original.isEliminado()) {
                return "redirect:/gestionInmuebles";
            }

            // Respetar flag eliminado
            inmueble.setEliminado(original.isEliminado());

            if (original.getDisponibilidad() == null) {
                original.setDisponibilidad(new Disponibilidad());
            }

            // Si el usuario intenta cambiar las fechas, solo bloqueamos si eso deja reservas activas fuera
            Disponibilidad nueva = inmueble.getDisponibilidad();
            Disponibilidad vieja = original.getDisponibilidad();

            // Si no vienen fechas (por si acaso), mantenemos las viejas
            if (nueva.getFechaInicio() == null) nueva.setFechaInicio(vieja.getFechaInicio());
            if (nueva.getFechaFin() == null) nueva.setFechaFin(vieja.getFechaFin());

            boolean hayReservaActiva = reservaDAO.existsReservaActiva(inmueble.getId(), new java.util.Date());

            if (hayReservaActiva) {
                // 1) Si el nuevo rango deja alguna reserva activa fuera -> NO guardar cambios de fechas
                boolean rompe = false;
                if (nueva.getFechaInicio() != null && nueva.getFechaFin() != null) {
                    rompe = reservaDAO.existsReservaActivaFueraDeRango(
                            inmueble.getId(),
                            new java.util.Date(),
                            nueva.getFechaInicio(),
                            nueva.getFechaFin()
                    );
                }

                if (rompe) {
                    model.addAttribute("error",
                            "⚠️ No puedes cambiar el rango de disponibilidad: hay reservas activas que quedarían fuera del nuevo rango.");
                    model.addAttribute("inmueble", original); // volvemos con lo que está guardado realmente
                    return "forminmueble";
                }

                // 
            }
        }

        // Guardar inmueble
        inmuebleDAO.save(inmueble);

        // Guardar disponibilidad (mismo objeto asociado al inmueble)
        Disponibilidad disponibilidad = inmueble.getDisponibilidad();
        if (disponibilidad != null && disponibilidad.getFechaInicio() != null && disponibilidad.getFechaFin() != null) {
            disponibilidad.setInmueble(inmueble);
            disponibilidadDAO.save(disponibilidad);
        }

        try {
            if (esNuevo) {
                gestorNotificaciones.inmueblePublicado(propietario, inmueble);
            } else {
                gestorNotificaciones.inmuebleActualizado(propietario, inmueble);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al enviar notificación de inmueble: " + e.getMessage());
        }

        return "redirect:/gestionInmuebles";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarInmueble(@PathVariable Long id, Model model, @SessionAttribute("usuario") Propietario propietario) {

        Optional<Inmueble> optionalInmueble = inmuebleDAO.findById(id);
        if (optionalInmueble.isEmpty()) return "redirect:/gestionInmuebles";

        Inmueble inmueble = optionalInmueble.get();

        // Seguridad: solo dueño y no eliminado
        if (!inmueble.getPropietario().getId().equals(propietario.getId()) || inmueble.isEliminado()) {
            return "redirect:/gestionInmuebles";
        }

        boolean hayActiva = reservaDAO.existsReservaActiva(id, new java.util.Date());

        if (!hayActiva) {
            // NO hay reservas activas -> borrado real
            List<Disponibilidad> disponibilidades = disponibilidadDAO.findByInmueble(inmueble);
            disponibilidadDAO.deleteAll(disponibilidades);

            inmuebleDAO.delete(inmueble);

            gestorNotificaciones.enviar(propietario, "INMUEBLE_ELIMINADO",
                    "🏚️ Has eliminado tu inmueble: " + inmueble.getDireccion());

            return "redirect:/gestionInmuebles";
        }

        // Hay reserva activa -> volvemos a la misma vista con aviso
        List<Inmueble> inmuebles = inmuebleDAO.findByPropietario(propietario)
                .stream()
                .filter(i -> i != null && !i.isEliminado())
                .collect(Collectors.toList());

        model.addAttribute("inmuebles", inmuebles);
        model.addAttribute("confirmarEliminacion", true);
        model.addAttribute("inmuebleAEliminar", inmueble);

        return "gestionInmuebles";
    }

    @PostMapping("/eliminarConfirmado")
    public String eliminarConfirmado(@RequestParam Long inmuebleId, @SessionAttribute("usuario") Propietario propietario) {

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return "redirect:/gestionInmuebles";

        if (!inmueble.getPropietario().getId().equals(propietario.getId()) || inmueble.isEliminado()) {
            return "redirect:/gestionInmuebles";
        }

       
        inmueble.setEliminado(true);
        inmuebleDAO.save(inmueble);

        gestorNotificaciones.enviar(propietario, "INMUEBLE_ELIMINADO",
                "🏚️ Has retirado tu inmueble: " + inmueble.getDireccion()
                        + ". Las reservas activas se mantienen.");

        return "redirect:/gestionInmuebles";
    }

    @GetMapping("/resultados")
    public String mostrarResultados(Model model) {
        model.addAttribute("inmuebles", inmuebleDAO.findAll());
        return "resultados";
    }

    @GetMapping("/detalle/{id}")
    public String mostrarDetalle(@PathVariable("id") Long id, Model model) {
        Inmueble inmueble = inmuebleDAO.findById(id).orElse(null);
        model.addAttribute("inmueble", inmueble);
        return "detalleInmueble";
    }
    
}
