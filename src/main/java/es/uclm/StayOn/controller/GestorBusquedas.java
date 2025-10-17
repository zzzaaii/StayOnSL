package es.uclm.StayOn.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.persistence.InmuebleDAO;

import java.util.List;
import java.util.ArrayList;

@Controller
public class GestorBusquedas {
	
	@Autowired
    private InmuebleDAO inmuebleDAO;

    // Muestra la página de búsqueda inicial
    @GetMapping("/buscarInmuebles")
    public String mostrarPaginaBusqueda(Model model) {
        // Inicialmente, la lista de resultados está vacía
        model.addAttribute("resultados", new ArrayList<Inmueble>());
        return "busqueda";
    }

    // Procesa la búsqueda enviada desde el formulario
    @PostMapping("/buscar")
    public String buscarAlojamientos(@RequestParam(required = false) String destino, Model model) {
        
        List<Inmueble> resultados;

        if (destino != null && !destino.trim().isEmpty()) {
            // Si se proporciona un destino, busca por él
            resultados = inmuebleDAO.findByDireccionContainingIgnoreCase(destino);
        } else {
            // Si no, muestra todos los inmuebles (o los más populares, etc.)
            resultados = inmuebleDAO.findAll();
        }

        model.addAttribute("resultados", resultados);
        model.addAttribute("busquedaRealizada", true); // Para mostrar un mensaje si no hay resultados
        return "busqueda";
    }
}
