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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/gestionInmuebles")
@SessionAttributes("usuario")
public class GestorInmuebles {

    @Autowired
    private InmuebleDAO inmuebleDAO;

    @Autowired
    private DisponibilidadDAO disponibilidadDAO;

    @Autowired
    private GestorNotificaciones gestorNotificaciones;

    @GetMapping
    public String listarInmuebles(Model model, @SessionAttribute("usuario") Propietario propietario) {
        List<Inmueble> inmuebles = inmuebleDAO.findByPropietario(propietario);
        model.addAttribute("inmuebles", inmuebles);
        return "gestionInmuebles";
    }

    @GetMapping("/nuevo")
    public String nuevoInmueble(Model model) {
        model.addAttribute("inmueble", new Inmueble());
        model.addAttribute("disponibilidad", new Disponibilidad());
        return "forminmueble";
    }

    @PostMapping("/guardar")
    public String guardarInmueble(@ModelAttribute Inmueble inmueble,
                                  @SessionAttribute("usuario") Propietario propietario,
                                  @ModelAttribute("disponibilidad") Disponibilidad disponibilidad) {

        boolean esNuevo = (inmueble.getId() == null);

        if (inmueble.getTipo() == null || inmueble.getTipo().isBlank()) inmueble.setTipo("Vivienda");
        if (inmueble.getDireccion() == null || inmueble.getDireccion().isBlank()) inmueble.setDireccion("Sin dirección");
        if (inmueble.getCiudad() == null || inmueble.getCiudad().isBlank()) inmueble.setCiudad("Sin ciudad");
        if (inmueble.getPrecioPorNoche() == null) inmueble.setPrecioPorNoche(0.1);

        inmueble.setPropietario(propietario);
        inmuebleDAO.save(inmueble);

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

    @GetMapping("/editar/{id}")
    public String editarInmueble(@PathVariable Long id, Model model, @SessionAttribute("usuario") Propietario propietario) {
        Optional<Inmueble> optionalInmueble = inmuebleDAO.findById(id);
        if (optionalInmueble.isEmpty() || !optionalInmueble.get().getPropietario().getId().equals(propietario.getId())) {
            return "redirect:/gestionInmuebles";
        }

        model.addAttribute("inmueble", optionalInmueble.get());
        List<Disponibilidad> disponibilidades = disponibilidadDAO.findByInmueble(optionalInmueble.get());
        model.addAttribute("disponibilidad", disponibilidades.isEmpty() ? new Disponibilidad() : disponibilidades.get(0));

        return "forminmueble";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarInmueble(@PathVariable Long id, @SessionAttribute("usuario") Propietario propietario) {
        Optional<Inmueble> optionalInmueble = inmuebleDAO.findById(id);
        if (optionalInmueble.isPresent() && optionalInmueble.get().getPropietario().getId().equals(propietario.getId())) {
            List<Disponibilidad> disponibilidades = disponibilidadDAO.findByInmueble(optionalInmueble.get());
            disponibilidadDAO.deleteAll(disponibilidades);
            inmuebleDAO.delete(optionalInmueble.get());
            gestorNotificaciones.enviar(propietario, "INMUEBLE_ELIMINADO",
                    "🏚️ Has eliminado tu inmueble: " + optionalInmueble.get().getDireccion());
        }
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
