package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.persistence.InmuebleDAO;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/gestionInmuebles")
@SessionAttributes("usuario")
public class GestorInmuebles {

    @Autowired
    private InmuebleDAO inmuebleDAO;

    @GetMapping
    public String listarInmuebles(Model model, @SessionAttribute("usuario") Propietario propietario) {
       List<Inmueble> inmuebles = inmuebleDAO.findByPropietario(propietario);
        model.addAttribute("inmuebles", inmuebles);
        return "gestionInmuebles";
    }

    @GetMapping("/nuevo")
    public String nuevoInmueble(Model model) {
        model.addAttribute("inmueble", new Inmueble());
        return "formInmueble";
    }

    @PostMapping("/guardar")
    public String guardarInmueble(@ModelAttribute Inmueble inmueble, @SessionAttribute("usuario") Propietario propietario) {
        inmueble.setPropietario(propietario);
        inmuebleDAO.save(inmueble);
        return "redirect:/gestionInmuebles";
    }

    @GetMapping("/editar/{id}")
    public String editarInmueble(@PathVariable Long id, Model model, @SessionAttribute("usuario") Propietario propietario) {
        Optional<Inmueble> optionalInmueble = inmuebleDAO.findById(id);
        if (optionalInmueble.isEmpty() || !optionalInmueble.get().getPropietario().equals(propietario)) {
            return "redirect:/gestionInmuebles"; // Evita que un propietario edite inmuebles ajenos
        }
        model.addAttribute("inmueble", optionalInmueble.get());
        return "formInmueble";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarInmueble(@PathVariable Long id, @SessionAttribute("usuario") Propietario propietario) {
        Optional<Inmueble> optionalInmueble = inmuebleDAO.findById(id);
        if (optionalInmueble.isPresent() && optionalInmueble.get().getPropietario().equals(propietario)) {
            inmuebleDAO.deleteById(id);
        }
        return "redirect:/gestionInmuebles";
    }
}
