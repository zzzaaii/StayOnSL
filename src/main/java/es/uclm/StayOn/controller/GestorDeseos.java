package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.persistence.DeseoDAO;
import es.uclm.StayOn.persistence.InmuebleDAO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class GestorDeseos {

     static final String ATTR_USUARIO = "usuario";

    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_INICIO = "redirect:/inicio";
    private static final String REDIRECT_MIS_DESEOS = "redirect:/misDeseos";

    private final DeseoDAO deseoDAO;
    private final InmuebleDAO inmuebleDAO;

    public GestorDeseos(DeseoDAO deseoDAO, InmuebleDAO inmuebleDAO) {
        this.deseoDAO = deseoDAO;
        this.inmuebleDAO = inmuebleDAO;
    }

  
    @GetMapping("/misDeseos")
    public String misDeseos(HttpSession session, Model model) {

        Usuario u = (Usuario) session.getAttribute(ATTR_USUARIO);
        if (u == null) return REDIRECT_LOGIN;
        if (!(u instanceof Inquilino inquilino)) return REDIRECT_INICIO;

        model.addAttribute("deseos", deseoDAO.findByInquilino(inquilino));
        return "misDeseos";
    }

    
    @PostMapping("/deseos/add")
    public String addDeseo(@RequestParam("inmuebleId") Long inmuebleId,
                           HttpSession session) {

        Usuario u = (Usuario) session.getAttribute(ATTR_USUARIO);
        if (u == null) return REDIRECT_LOGIN;
        if (!(u instanceof Inquilino inquilino)) return REDIRECT_INICIO;

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return REDIRECT_MIS_DESEOS;

        
        if (deseoDAO.findByInquilinoAndInmueble(inquilino, inmueble).isEmpty()) {
            Deseo d = new Deseo();
            d.setInquilino(inquilino);
            d.setInmueble(inmueble);
            deseoDAO.save(d);
        }

        return REDIRECT_MIS_DESEOS;
    }

    
    @PostMapping("/deseos/remove")
    public String removeDeseo(@RequestParam("inmuebleId") Long inmuebleId,
                              HttpSession session) {

        Usuario u = (Usuario) session.getAttribute(ATTR_USUARIO);
        if (u == null) return REDIRECT_LOGIN;
        if (!(u instanceof Inquilino inquilino)) return REDIRECT_INICIO;

        Inmueble inmueble = inmuebleDAO.findById(inmuebleId).orElse(null);
        if (inmueble == null) return REDIRECT_MIS_DESEOS;

        deseoDAO.findByInquilinoAndInmueble(inquilino, inmueble)
                .ifPresent(deseoDAO::delete);

        return REDIRECT_MIS_DESEOS;
    }
}
