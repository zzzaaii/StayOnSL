package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.persistence.DeseoDAO;
import es.uclm.StayOn.persistence.InmuebleDAO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class GestorDeseos {

    @Autowired private DeseoDAO deseoDAO;
    @Autowired private InmuebleDAO inmuebleDAO;

    //  Ver mis deseos
    @GetMapping("/misDeseos")
    public String misDeseos(HttpSession session, Model model) {

        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null) return "redirect:/login";
        if (!(u instanceof Inquilino inquilino)) return "redirect:/inicio";

        model.addAttribute("deseos", deseoDAO.findByInquilino(inquilino));
        return "misDeseos";
    }

    // Añadir a deseos
    @PostMapping("/deseos/add")
    public String addDeseo(@RequestParam("inmuebleId") Long inmuebleId, HttpSession session) {

        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null) return "redirect:/login";
        if (!(u instanceof Inquilino inquilino)) return "redirect:/inicio";

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return "redirect:/misDeseos";

        // Evitar duplicados
        if (deseoDAO.findByInquilinoAndInmueble(inquilino, inmueble).isEmpty()) {
            Deseo d = new Deseo();
            d.setInquilino(inquilino);
            d.setInmueble(inmueble);
            deseoDAO.save(d);
        }

        return "redirect:/misDeseos";
    }

    // Quitar de deseos
    @PostMapping("/deseos/remove")
    public String removeDeseo(@RequestParam("inmuebleId") Long inmuebleId, HttpSession session) {

        Usuario u = (Usuario) session.getAttribute("usuario");
        if (u == null) return "redirect:/login";
        if (!(u instanceof Inquilino inquilino)) return "redirect:/inicio";

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return "redirect:/misDeseos";

        deseoDAO.findByInquilinoAndInmueble(inquilino, inmueble)
                .ifPresent(deseoDAO::delete);

        return "redirect:/misDeseos";
    }
}
