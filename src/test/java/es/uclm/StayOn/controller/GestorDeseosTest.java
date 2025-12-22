package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.persistence.DeseoDAO;
import es.uclm.StayOn.persistence.InmuebleDAO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GestorDeseosTest {

    private MockMvc mockMvc;

    @Mock
    private DeseoDAO deseoDAO;

    @Mock
    private InmuebleDAO inmuebleDAO;

    @InjectMocks
    private GestorDeseos gestorDeseos;

    @BeforeEach
   
    void setUp() {
        MockitoAnnotations.openMocks(this);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp"); 

        mockMvc = MockMvcBuilders
                .standaloneSetup(gestorDeseos)
                .setViewResolvers(viewResolver)
                .build();
    }


    private static Inquilino inquilinoMinimo(Long id, String nombre) {
        Inquilino i = new Inquilino();
        i.setId(id);
        i.setNombre(nombre);
        i.setLogin("inq" + id);
        i.setPass("pass");
        return i;
    }

    private static Usuario usuarioNoInquilinoMinimo(Long id, String nombre) {
        Usuario u = new Usuario() {};
        u.setId(id);
        u.setNombre(nombre);
        u.setLogin("usr" + id);
        u.setPass("pass");
        return u;
    }

    private static Inmueble inmuebleBasico(Long id, String tipo, String ciudad, String direccion) {
        Inmueble inm = new Inmueble();
        inm.setId(id);
        inm.setTipo(tipo);
        inm.setCiudad(ciudad);
        inm.setDireccion(direccion);
        inm.setDescripcion("Desc");
        inm.setPrecioPorNoche(50.0);
        inm.setEliminado(false);
        return inm;
    }

    private static Deseo deseoBasico(Long id, Inquilino inq, Inmueble inm) {
        Deseo d = new Deseo();
        d.setId(id);
        d.setInquilino(inq);
        d.setInmueble(inm);
        return d;
    }

    @Test
    @DisplayName("GET /misDeseos sin usuario en sesión -> redirect /login")
    void misDeseos_sinUsuario_redirectLogin() throws Exception {
        mockMvc.perform(get("/misDeseos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(deseoDAO);
    }

    @Test
    @DisplayName("GET /misDeseos usuario no inquilino -> redirect /inicio")
    void misDeseos_usuarioNoInquilino_redirectInicio() throws Exception {
        Usuario u = usuarioNoInquilinoMinimo(1L, "Pepe");

        mockMvc.perform(get("/misDeseos")
                        .sessionAttr("usuario", u))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verifyNoInteractions(deseoDAO);
    }

    @Test
    @DisplayName("GET /misDeseos inquilino OK -> vista misDeseos con atributo deseos")
    void misDeseos_inquilinoOk_devuelveVistaYModel() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(1L, "Vivienda", "Madrid", "Calle A");
        Deseo d = deseoBasico(100L, inq, inm);

        when(deseoDAO.findByInquilino(inq)).thenReturn(List.of(d));

        mockMvc.perform(get("/misDeseos")
                        .sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(view().name("misDeseos"))
                .andExpect(model().attributeExists("deseos"));

        verify(deseoDAO, times(1)).findByInquilino(inq);
    }

    @Test
    @DisplayName("POST /deseos/add sin sesión -> redirect /login")
    void addDeseo_sinUsuario_redirectLogin() throws Exception {
        mockMvc.perform(post("/deseos/add")
                        .param("inmuebleId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(inmuebleDAO, deseoDAO);
    }

    @Test
    @DisplayName("POST /deseos/add usuario no inquilino -> redirect /inicio")
    void addDeseo_usuarioNoInquilino_redirectInicio() throws Exception {
        Usuario u = usuarioNoInquilinoMinimo(1L, "Pepe");

        mockMvc.perform(post("/deseos/add")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", u))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verifyNoInteractions(inmuebleDAO, deseoDAO);
    }

    @Test
    @DisplayName("POST /deseos/add inmueble no existe -> redirect /misDeseos")
    void addDeseo_inmuebleNoExiste_redirectMisDeseos() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        when(inmuebleDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/deseos/add")
                        .param("inmuebleId", "99")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misDeseos"));

        verify(inmuebleDAO, times(1)).findById(99L);
        verifyNoInteractions(deseoDAO);
    }

    @Test
    @DisplayName("POST /deseos/add si no hay duplicado -> guarda Deseo y redirect /misDeseos")
    void addDeseo_sinDuplicado_guarda() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(1L, "Vivienda", "Madrid", "Calle A");

        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(inm));
        when(deseoDAO.findByInquilinoAndInmueble(inq, inm)).thenReturn(Optional.empty());

        mockMvc.perform(post("/deseos/add")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misDeseos"));

        verify(deseoDAO, times(1)).save(any(Deseo.class));
    }

    @Test
    @DisplayName("POST /deseos/add si ya existe duplicado -> NO guarda y redirect /misDeseos")
    void addDeseo_conDuplicado_noGuarda() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(1L, "Vivienda", "Madrid", "Calle A");
        Deseo existente = deseoBasico(100L, inq, inm);

        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(inm));
        when(deseoDAO.findByInquilinoAndInmueble(inq, inm)).thenReturn(Optional.of(existente));

        mockMvc.perform(post("/deseos/add")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misDeseos"));

        verify(deseoDAO, never()).save(any(Deseo.class));
    }

    @Test
    @DisplayName("POST /deseos/remove sin sesión -> redirect /login")
    void removeDeseo_sinUsuario_redirectLogin() throws Exception {
        mockMvc.perform(post("/deseos/remove")
                        .param("inmuebleId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(inmuebleDAO, deseoDAO);
    }

    @Test
    @DisplayName("POST /deseos/remove usuario no inquilino -> redirect /inicio")
    void removeDeseo_usuarioNoInquilino_redirectInicio() throws Exception {
        Usuario u = usuarioNoInquilinoMinimo(1L, "Pepe");

        mockMvc.perform(post("/deseos/remove")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", u))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verifyNoInteractions(inmuebleDAO, deseoDAO);
    }

    @Test
    @DisplayName("POST /deseos/remove inmueble no existe -> redirect /misDeseos")
    void removeDeseo_inmuebleNoExiste_redirectMisDeseos() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        when(inmuebleDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/deseos/remove")
                        .param("inmuebleId", "99")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misDeseos"));

        verify(inmuebleDAO, times(1)).findById(99L);
        verifyNoInteractions(deseoDAO);
    }

    @Test
    @DisplayName("POST /deseos/remove si existe deseo -> delete y redirect /misDeseos")
    void removeDeseo_existeDeseo_borra() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(1L, "Vivienda", "Madrid", "Calle A");
        Deseo existente = deseoBasico(100L, inq, inm);

        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(inm));
        when(deseoDAO.findByInquilinoAndInmueble(inq, inm)).thenReturn(Optional.of(existente));

        mockMvc.perform(post("/deseos/remove")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misDeseos"));

        verify(deseoDAO, times(1)).delete(existente);
    }

    @Test
    @DisplayName("POST /deseos/remove si NO existe deseo -> no borra y redirect /misDeseos")
    void removeDeseo_noExisteDeseo_noBorra() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(1L, "Vivienda", "Madrid", "Calle A");

        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(inm));
        when(deseoDAO.findByInquilinoAndInmueble(inq, inm)).thenReturn(Optional.empty());

        mockMvc.perform(post("/deseos/remove")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misDeseos"));

        verify(deseoDAO, never()).delete(any(Deseo.class));
    }

    @Test
    void gettersAndSetters_work() {
        Deseo d = new Deseo();

        Long id = 123L;
        Inquilino inq = new Inquilino();
        inq.setId(10L);

        Inmueble inm = new Inmueble();
        inm.setId(20L);

        d.setId(id);
        d.setInquilino(inq);
        d.setInmueble(inm);

        assertThat(d.getId()).isEqualTo(123L);
        assertThat(d.getInquilino()).isSameAs(inq);
        assertThat(d.getInmueble()).isSameAs(inm);
    }
}
