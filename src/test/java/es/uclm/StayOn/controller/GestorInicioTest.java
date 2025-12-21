package es.uclm.StayOn.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GestorInicio.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class GestorInicioTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / -> devuelve vista Inicio")
    void raiz_devuelveInicio() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("Inicio"));
    }

    @Test
    @DisplayName("GET /inicio -> devuelve vista Inicio")
    void inicio_devuelveInicio() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(view().name("Inicio"));
    }

    @Test
    @DisplayName("GET /inicioInquilino -> devuelve vista inicioInquilino")
    void inicioInquilino_devuelveVista() throws Exception {
        mockMvc.perform(get("/inicioInquilino"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicioInquilino"));
    }

    @Test
    @DisplayName("GET /inicioPropietario -> devuelve vista inicioPropietario")
    void inicioPropietario_devuelveVista() throws Exception {
        mockMvc.perform(get("/inicioPropietario"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicioPropietario"));
    }
}
