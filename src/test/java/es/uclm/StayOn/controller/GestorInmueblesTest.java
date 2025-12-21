package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.Disponibilidad;
import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.persistence.DisponibilidadDAO;
import es.uclm.StayOn.persistence.InmuebleDAO;
import es.uclm.StayOn.persistence.ReservaDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GestorInmuebles.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class GestorInmueblesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InmuebleDAO inmuebleDAO;

    @MockBean
    private DisponibilidadDAO disponibilidadDAO;

    @MockBean
    private ReservaDAO reservaDAO;

    @MockBean
    private GestorNotificaciones gestorNotificaciones;

    private static Date d(String yyyyMmDd) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(yyyyMmDd);
    }

    private static Propietario propietario(Long id, String nombre) {
        Propietario p = new Propietario();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }

    private static Inmueble inmueble(Long id, Propietario p, boolean eliminado) {
        Inmueble i = new Inmueble();
        i.setId(id);
        i.setPropietario(p);
        i.setEliminado(eliminado);
        i.setTipo("Vivienda");
        i.setCiudad("Madrid");
        i.setDireccion("Calle " + id);
        i.setPrecioPorNoche(50.0);
        return i;
    }

    private static Disponibilidad disp(Date ini, Date fin) {
        Disponibilidad d = new Disponibilidad();
        d.setFechaInicio(ini);
        d.setFechaFin(fin);
        return d;
    }

   

    @Test
    @DisplayName("GET /gestionInmuebles -> lista inmuebles del propietario filtrando nulls y eliminados")
    void listarInmuebles_filtraEliminadosYNulls() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble ok = inmueble(10L, p, false);
        Inmueble eliminado = inmueble(11L, p, true);

        when(inmuebleDAO.findByPropietario(p)).thenReturn(Arrays.asList(ok, null, eliminado));

        mockMvc.perform(get("/gestionInmuebles")
                        .sessionAttr("usuario", p))
                .andExpect(status().isOk())
                .andExpect(view().name("gestionInmuebles"))
                .andExpect(model().attributeExists("inmuebles"));

       
        verify(inmuebleDAO, times(1)).findByPropietario(p);
    }

    
    @Test
    @DisplayName("GET /gestionInmuebles/nuevo -> devuelve forminmueble con inmueble y disponibilidad inicializada")
    void nuevoInmueble_ok() throws Exception {
        var res = mockMvc.perform(get("/gestionInmuebles/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("forminmueble"))
                .andExpect(model().attributeExists("inmueble"))
                .andReturn();

        Object inm = Objects.requireNonNull(res.getModelAndView()).getModel().get("inmueble");
        assertThat(inm).isInstanceOf(Inmueble.class);
        Inmueble i = (Inmueble) inm;
        assertThat(i.getDisponibilidad()).isNotNull();
    }

    

    @Test
    @DisplayName("GET /gestionInmuebles/editar/{id} -> si no existe redirect a /gestionInmuebles")
    void editar_noExiste_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        when(inmuebleDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/gestionInmuebles/editar/99")
                        .sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));
    }

    @Test
    @DisplayName("GET /gestionInmuebles/editar/{id} -> si no es el dueño redirect a /gestionInmuebles")
    void editar_noDuenio_redirect() throws Exception {
        Propietario duenio = propietario(1L, "Prop");
        Propietario otro = propietario(2L, "Otro");
        Inmueble i = inmueble(10L, otro, false);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/gestionInmuebles/editar/10")
                        .sessionAttr("usuario", duenio))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));
    }

    @Test
    @DisplayName("GET /gestionInmuebles/editar/{id} -> si está eliminado redirect a /gestionInmuebles")
    void editar_eliminado_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Inmueble i = inmueble(10L, p, true);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/gestionInmuebles/editar/10")
                        .sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));
    }

    @Test
    @DisplayName("GET /gestionInmuebles/editar/{id} -> ok devuelve forminmueble con inmueble (y disponibilidad no null)")
    void editar_ok() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Inmueble i = inmueble(10L, p, false);
        i.setDisponibilidad(null);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        var res = mockMvc.perform(get("/gestionInmuebles/editar/10")
                        .sessionAttr("usuario", p))
                .andExpect(status().isOk())
                .andExpect(view().name("forminmueble"))
                .andExpect(model().attributeExists("inmueble"))
                .andReturn();

        Inmueble modelInm = (Inmueble) Objects.requireNonNull(res.getModelAndView()).getModel().get("inmueble");
        assertThat(modelInm.getDisponibilidad()).isNotNull();
    }

   

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (nuevo) -> aplica defaults, guarda y notifica publicado")
    void guardar_nuevo_aplicaDefaults_yNotificaPublicado() throws Exception {
        Propietario p = propietario(1L, "Prop");

      
        ArgumentCaptor<Inmueble> captor = ArgumentCaptor.forClass(Inmueble.class);
        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, times(1)).save(captor.capture());
        Inmueble guardado = captor.getValue();

        assertThat(guardado.getPropietario()).isNotNull();
        assertThat(guardado.getPropietario().getId()).isEqualTo(1L);

       
        assertThat(guardado.getTipo()).isEqualTo("Vivienda");
        assertThat(guardado.getDireccion()).isEqualTo("Sin dirección");
        assertThat(guardado.getCiudad()).isEqualTo("Sin ciudad");
        assertThat(guardado.getPrecioPorNoche()).isEqualTo(0.0);

       
        verify(disponibilidadDAO, never()).save(any(Disponibilidad.class));

        verify(gestorNotificaciones, times(1)).inmueblePublicado(eq(p), any(Inmueble.class));
        verify(gestorNotificaciones, never()).inmuebleActualizado(any(), any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> si hay reservas activas y el nuevo rango las deja fuera, muestra error y NO guarda")
    void guardar_editar_reservasActivas_rompeRango_error() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble original = inmueble(10L, p, false);
        original.setDisponibilidad(new Disponibilidad());

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));
        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(true);
        when(reservaDAO.existsReservaActivaFueraDeRango(eq(10L), any(Date.class), any(Date.class), any(Date.class))).thenReturn(true);

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento")
                        .param("direccion", "Nueva Direccion")
                        .param("ciudad", "Toledo")
                        .param("precioPorNoche", "99.9")
                        .param("disponibilidad.fechaInicio", "2025-12-10")
                        .param("disponibilidad.fechaFin", "2025-12-20")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("forminmueble"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("inmueble"));

        verify(inmuebleDAO, never()).save(any(Inmueble.class));
        verify(disponibilidadDAO, never()).save(any(Disponibilidad.class));
        verify(gestorNotificaciones, never()).inmuebleActualizado(any(), any());
        verify(gestorNotificaciones, never()).inmueblePublicado(any(), any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> reservas activas pero NO rompe rango: guarda y notifica actualizado")
    void guardar_editar_reservasActivas_noRompe_guardarOk() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble original = inmueble(10L, p, false);
        original.setDisponibilidad(disp(d("2025-12-01"), d("2025-12-31")));

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));
        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(true);
        when(reservaDAO.existsReservaActivaFueraDeRango(eq(10L), any(Date.class), any(Date.class), any(Date.class))).thenReturn(false);

        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disponibilidadDAO.save(any(Disponibilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento")
                        .param("direccion", "Nueva Direccion")
                        .param("ciudad", "Toledo")
                        .param("precioPorNoche", "99.9")
                        .param("disponibilidad.fechaInicio", "2025-12-10")
                        .param("disponibilidad.fechaFin", "2025-12-20")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, times(1)).save(any(Inmueble.class));
        verify(disponibilidadDAO, times(1)).save(any(Disponibilidad.class));
        verify(gestorNotificaciones, times(1)).inmuebleActualizado(eq(p), any(Inmueble.class));
        verify(gestorNotificaciones, never()).inmueblePublicado(any(), any());
    }

 
    

    @Test
    @DisplayName("GET /gestionInmuebles/eliminar/{id} -> sin reservas activas: borra disponibilidades, borra inmueble y notifica")
    void eliminar_sinReservasActivas_borradoReal() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble i = inmueble(10L, p, false);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(false);

        List<Disponibilidad> disps = List.of(new Disponibilidad(), new Disponibilidad());
        when(disponibilidadDAO.findByInmueble(i)).thenReturn(disps);

        mockMvc.perform(get("/gestionInmuebles/eliminar/10")
                        .sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(disponibilidadDAO, times(1)).findByInmueble(i);
        verify(disponibilidadDAO, times(1)).deleteAll(disps);
        verify(inmuebleDAO, times(1)).delete(i);

        verify(gestorNotificaciones, times(1)).enviar(eq(p), eq("INMUEBLE_ELIMINADO"), contains("Has eliminado tu inmueble"));
    }

    @Test
    @DisplayName("GET /gestionInmuebles/eliminar/{id} -> con reservas activas: vuelve a gestionInmuebles con flags de confirmación")
    void eliminar_conReservasActivas_pideConfirmacion() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble i = inmueble(10L, p, false);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(true);

        when(inmuebleDAO.findByPropietario(p)).thenReturn(List.of(i));

        mockMvc.perform(get("/gestionInmuebles/eliminar/10")
                        .sessionAttr("usuario", p))
                .andExpect(status().isOk())
                .andExpect(view().name("gestionInmuebles"))
                .andExpect(model().attributeExists("inmuebles"))
                .andExpect(model().attribute("confirmarEliminacion", true))
                .andExpect(model().attributeExists("inmuebleAEliminar"));

        verify(inmuebleDAO, never()).delete(any(Inmueble.class));
        verify(disponibilidadDAO, never()).deleteAll(anyList());
    }

    

    @Test
    @DisplayName("POST /gestionInmuebles/eliminarConfirmado -> marca eliminado=true, guarda y notifica")
    void eliminarConfirmado_ok() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble i = inmueble(10L, p, false);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));
        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/eliminarConfirmado")
                        .sessionAttr("usuario", p)
                        .param("inmuebleId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        ArgumentCaptor<Inmueble> captor = ArgumentCaptor.forClass(Inmueble.class);
        verify(inmuebleDAO, times(1)).save(captor.capture());
        assertThat(captor.getValue().isEliminado()).isTrue();

        verify(gestorNotificaciones, times(1)).enviar(eq(p), eq("INMUEBLE_ELIMINADO"), contains("Has retirado tu inmueble"));
    }

    

    @Test
    @DisplayName("GET /gestionInmuebles/resultados -> vista resultados con inmuebles=findAll()")
    void resultados_ok() throws Exception {
        when(inmuebleDAO.findAll()).thenReturn(List.of(new Inmueble(), new Inmueble()));

        mockMvc.perform(get("/gestionInmuebles/resultados"))
                .andExpect(status().isOk())
                .andExpect(view().name("resultados"))
                .andExpect(model().attributeExists("inmuebles"));

        verify(inmuebleDAO, times(1)).findAll();
    }


    @Test
    @DisplayName("GET /gestionInmuebles/detalle/{id} -> vista detalleInmueble con inmueble (o null si no existe)")
    void detalle_ok() throws Exception {
        Inmueble i = new Inmueble();
        i.setId(10L);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/gestionInmuebles/detalle/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("detalleInmueble"))
                .andExpect(model().attributeExists("inmueble"));

        verify(inmuebleDAO, times(1)).findById(10L);
    }
    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> si original no existe redirect")
    void guardar_editar_originalNoExiste_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).save(any());
        verify(disponibilidadDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> si no es dueño redirect")
    void guardar_editar_noDuenio_redirect() throws Exception {
        Propietario duenio = propietario(1L, "Prop");
        Propietario otro = propietario(2L, "Otro");

        Inmueble original = inmueble(10L, otro, false);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", duenio)
                        .param("id", "10")
                        .param("tipo", "Apartamento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).save(any());
        verify(disponibilidadDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> si original está eliminado redirect")
    void guardar_editar_originalEliminado_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble original = inmueble(10L, p, true);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).save(any());
        verify(disponibilidadDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> sin reservas activas: guarda y notifica actualizado")
    void guardar_editar_sinReservasActivas_guardaYNotifica() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble original = inmueble(10L, p, false);
        original.setDisponibilidad(disp(d("2025-12-01"), d("2025-12-31")));
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));

        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(false);

        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disponibilidadDAO.save(any(Disponibilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento")
                        .param("direccion", "Nueva Direccion")
                        .param("ciudad", "Toledo")
                        .param("precioPorNoche", "99.9")
                        .param("disponibilidad.fechaInicio", "2025-12-10")
                        .param("disponibilidad.fechaFin", "2025-12-20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, times(1)).save(any(Inmueble.class));
        verify(disponibilidadDAO, times(1)).save(any(Disponibilidad.class));
        verify(gestorNotificaciones, times(1)).inmuebleActualizado(eq(p), any(Inmueble.class));
        verify(gestorNotificaciones, never()).inmueblePublicado(any(), any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> si no vienen fechas nuevas, conserva las antiguas (y guarda)")
    void guardar_editar_fechasNull_conservaViejas() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble original = inmueble(10L, p, false);
        original.setDisponibilidad(disp(d("2025-12-01"), d("2025-12-31")));
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));

        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(true);
        when(reservaDAO.existsReservaActivaFueraDeRango(eq(10L), any(Date.class), any(Date.class), any(Date.class)))
                .thenReturn(false);

        ArgumentCaptor<Disponibilidad> capDisp = ArgumentCaptor.forClass(Disponibilidad.class);

        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disponibilidadDAO.save(any(Disponibilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        
        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento")
                        .param("direccion", "Nueva Direccion")
                        .param("ciudad", "Toledo")
                        .param("precioPorNoche", "99.9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(disponibilidadDAO, times(1)).save(capDisp.capture());
        Disponibilidad guardada = capDisp.getValue();

       
        assertThat(guardada.getFechaInicio()).isEqualTo(d("2025-12-01"));
        assertThat(guardada.getFechaFin()).isEqualTo(d("2025-12-31"));
    }

    @Test
    @DisplayName("GET /gestionInmuebles/eliminar/{id} -> si no existe redirect")
    void eliminar_noExiste_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        when(inmuebleDAO.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/gestionInmuebles/eliminar/999").sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).delete(any());
    }

    @Test
    @DisplayName("GET /gestionInmuebles/eliminar/{id} -> si no es dueño redirect")
    void eliminar_noDuenio_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Propietario otro = propietario(2L, "Otro");
        Inmueble i = inmueble(10L, otro, false);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/gestionInmuebles/eliminar/10").sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).delete(any());
    }

    @Test
    @DisplayName("GET /gestionInmuebles/eliminar/{id} -> si está eliminado redirect")
    void eliminar_eliminado_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Inmueble i = inmueble(10L, p, true);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/gestionInmuebles/eliminar/10").sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).delete(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/eliminarConfirmado -> si inmueble no existe redirect")
    void eliminarConfirmado_noExiste_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/gestionInmuebles/eliminarConfirmado")
                        .sessionAttr("usuario", p)
                        .param("inmuebleId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/eliminarConfirmado -> si no es dueño redirect")
    void eliminarConfirmado_noDuenio_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Propietario otro = propietario(2L, "Otro");
        Inmueble i = inmueble(10L, otro, false);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/gestionInmuebles/eliminarConfirmado")
                        .sessionAttr("usuario", p)
                        .param("inmuebleId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/eliminarConfirmado -> si ya está eliminado redirect")
    void eliminarConfirmado_yaEliminado_redirect() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Inmueble i = inmueble(10L, p, true);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/gestionInmuebles/eliminarConfirmado")
                        .sessionAttr("usuario", p)
                        .param("inmuebleId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, never()).save(any());
    }
    @Test
    @DisplayName("GET /gestionInmuebles/editar/{id} -> ok cuando la disponibilidad ya existe (no se crea nueva)")
    void editar_ok_disponibilidadYaExiste_noSeReemplaza() throws Exception {
        Propietario p = propietario(1L, "Prop");
        Inmueble i = inmueble(10L, p, false);

        Disponibilidad disp = new Disponibilidad();
        i.setDisponibilidad(disp);

        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(i));

        var res = mockMvc.perform(get("/gestionInmuebles/editar/10")
                        .sessionAttr("usuario", p))
                .andExpect(status().isOk())
                .andExpect(view().name("forminmueble"))
                .andReturn();

        Inmueble modelInm = (Inmueble) Objects.requireNonNull(res.getModelAndView())
                .getModel().get("inmueble");

        
        assertThat(modelInm.getDisponibilidad()).isSameAs(disp);
    }
    @Test
    @DisplayName("GET /gestionInmuebles/eliminar/{id} -> con reservas activas filtra nulls y eliminados (cubre lambda)")
    void eliminar_conReservasActivas_filtraNullsYEliminados() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble objetivo = inmueble(10L, p, false);
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(objetivo));
        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(true);

        Inmueble ok = inmueble(20L, p, false);
        Inmueble eliminado = inmueble(21L, p, true);

        when(inmuebleDAO.findByPropietario(p)).thenReturn(Arrays.asList(ok, null, eliminado));

        var res = mockMvc.perform(get("/gestionInmuebles/eliminar/10")
                        .sessionAttr("usuario", p))
                .andExpect(status().isOk())
                .andExpect(view().name("gestionInmuebles"))
                .andExpect(model().attribute("confirmarEliminacion", true))
                .andExpect(model().attributeExists("inmuebleAEliminar"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Inmueble> inmuebles = (List<Inmueble>) Objects.requireNonNull(res.getModelAndView())
                .getModel().get("inmuebles");

        assertThat(inmuebles).containsExactly(ok);
    }


    @Test
    @DisplayName("POST /gestionInmuebles/guardar (nuevo) -> defaults cuando llegan strings blank")
    void guardar_nuevo_defaults_porBlank() throws Exception {
        Propietario p = propietario(1L, "Prop");

        ArgumentCaptor<Inmueble> captor = ArgumentCaptor.forClass(Inmueble.class);
        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("tipo", "   ")
                        .param("direccion", " ")
                        .param("ciudad", "\t"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO).save(captor.capture());
        Inmueble guardado = captor.getValue();

        assertThat(guardado.getTipo()).isEqualTo("Vivienda");
        assertThat(guardado.getDireccion()).isEqualTo("Sin dirección");
        assertThat(guardado.getCiudad()).isEqualTo("Sin ciudad");
    }
    @Test
    @DisplayName("POST /gestionInmuebles/guardar -> si disponibilidad tiene una fecha null, NO se guarda disponibilidad")
    void guardar_noGuardaDisponibilidad_siFaltaUnaFecha() throws Exception {
        Propietario p = propietario(1L, "Prop");

        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> {
            Inmueble inm = inv.getArgument(0);
            inm.setId(99L); 
            return inm;
        });

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("tipo", "Vivienda")
                        .param("direccion", "X")
                        .param("ciudad", "Y")
                        .param("precioPorNoche", "10.0")
                        .param("disponibilidad.fechaInicio", "2025-12-10")
                      
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(disponibilidadDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /gestionInmuebles/guardar (nuevo) -> si notificación falla, NO rompe (cubre catch)")
    void guardar_nuevo_notificacionFalla_noRevienta() throws Exception {
        Propietario p = propietario(1L, "Prop");

        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("boom")).when(gestorNotificaciones)
                .inmueblePublicado(any(), any());

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        
        verify(inmuebleDAO, times(1)).save(any(Inmueble.class));
    }
    @Test
    @DisplayName("POST /gestionInmuebles/guardar (nuevo) -> precioPorNoche null => default 0.1")
    void guardar_nuevo_precioNull_default01() throws Exception {
        Propietario p = propietario(1L, "Prop");

        ArgumentCaptor<Inmueble> captor = ArgumentCaptor.forClass(Inmueble.class);
        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        
                        .param("precioPorNoche", "")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO).save(captor.capture());
        Inmueble guardado = captor.getValue();

        assertThat(guardado.getPrecioPorNoche()).isEqualTo(0.1);
    }
    @Test
    @DisplayName("POST /gestionInmuebles/guardar (editar) -> si original.disponibilidad es null se inicializa y guarda")
    void guardar_editar_originalDisponibilidadNull_seInicializa() throws Exception {
        Propietario p = propietario(1L, "Prop");

        Inmueble original = inmueble(10L, p, false);
        original.setDisponibilidad(null); 
        when(inmuebleDAO.findById(10L)).thenReturn(Optional.of(original));

        when(reservaDAO.existsReservaActiva(eq(10L), any(Date.class))).thenReturn(false);

        when(inmuebleDAO.save(any(Inmueble.class))).thenAnswer(inv -> inv.getArgument(0));
        when(disponibilidadDAO.save(any(Disponibilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/gestionInmuebles/guardar")
                        .sessionAttr("usuario", p)
                        .param("id", "10")
                        .param("tipo", "Apartamento")
                        .param("direccion", "Dir")
                        .param("ciudad", "Ciudad")
                        .param("precioPorNoche", "10.0")
                      
                        .param("disponibilidad.fechaInicio", "2025-12-10")
                        .param("disponibilidad.fechaFin", "2025-12-20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/gestionInmuebles"));

        verify(inmuebleDAO, times(1)).save(any(Inmueble.class));
        verify(disponibilidadDAO, times(1)).save(any(Disponibilidad.class));
    }
    
    



}
