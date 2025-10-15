package es.uclm.StayOn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GestorInicio {

    // Mapea la raíz y /inicio a la vista Inicio.html
    @GetMapping({"/", "/inicio"})
    public String mostrarInicio() {
        return "Inicio"; // nombre del HTML sin extensión
    }

    // Mapea el inicio específico para inquilino
    @GetMapping("/inicioInquilino")
    public String mostrarInicioInquilino() {
        return "inicioInquilino"; // Thymeleaf
    }

    // Mapea el inicio específico para propietario
    @GetMapping("/inicioPropietario")
    public String mostrarInicioPropietario() {
        return "inicioPropietario"; // Thymeleaf
    }
}
