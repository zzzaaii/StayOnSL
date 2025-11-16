package es.uclm.StayOn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GestorInicio {

    @GetMapping({"/", "/inicio"})
    public String mostrarInicio() {
        return "Inicio";
    }

    @GetMapping("/inicioInquilino")
    public String mostrarInicioInquilino() {
        return "inicioInquilino";
    }

    @GetMapping("/inicioPropietario")
    public String mostrarInicioPropietario() {
        return "inicioPropietario";
    }
}
