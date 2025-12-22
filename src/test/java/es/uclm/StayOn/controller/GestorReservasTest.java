package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.persistence.ReservaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GestorReservasTest {

    private MockMvc mockMvc;

    @Mock
    private ReservaDAO reservaDAO;

    @Mock
    private GestorNotificaciones gestorNotificaciones;

    @InjectMocks
    private GestorReservas gestorReservas;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(gestorReservas)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("GET /misReservas -> lista reservas del inquilino y vista misReservas")
    void listarReservas_ok() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Reserva r1 = reservaBasica(1L, inq, inmuebleBasico(100L, propietarioMinimo(20L, "Prop")));
        when(reservaDAO.findByInquilino(inq)).thenReturn(List.of(r1));

        mockMvc.perform(get("/misReservas")
                        .sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(view().name("misReservas"))
                .andExpect(model().attributeExists("reservas"));

        verify(reservaDAO, times(1)).findByInquilino(inq);
    }

    @Test
    @DisplayName("GET /misReservas/propietario -> lista reservas del propietario y vista reservasPropietario")
    void listarReservasPropietario_ok() throws Exception {
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        Reserva r1 = reservaBasica(1L, inquilinoMinimo(10L, "Ana"), inm);

        when(reservaDAO.findByInmueblePropietario(prop)).thenReturn(List.of(r1));

        mockMvc.perform(get("/misReservas/propietario")
                        .sessionAttr("usuario", prop))
                .andExpect(status().isOk())
                .andExpect(view().name("reservasPropietario"))
                .andExpect(model().attributeExists("reservas"));

        verify(reservaDAO, times(1)).findByInmueblePropietario(prop);
    }

    @Test
    @DisplayName("GET /misReservas/nueva -> devuelve formReserva con atributo reserva")
    void nuevaReserva_ok() throws Exception {
        mockMvc.perform(get("/misReservas/nueva")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().isOk())
                .andExpect(view().name("formReserva"))
                .andExpect(model().attributeExists("reserva"));
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si faltan datos básicos redirige /misReservas")
    void guardarReserva_faltanDatos_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Reserva reserva = new Reserva();

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si fechaInicio no es before fechaFin redirige /misReservas")
    void guardarReserva_fechasInvalidas_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), false));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 10));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si no hay disponibilidad o fechas de disponibilidad redirige /misReservas")
    void guardarReserva_sinDisponibilidad_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(null);

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si cae fuera del rango de disponibilidad redirige /misReservas")
    void guardarReserva_fueraDeRango_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 6, 1), d(2026, 6, 30), false));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 28));
        reserva.setFechaFin(d(2026, 6, 2));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si solapa redirige /misReservas")
    void guardarReserva_solapa_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), false));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        when(reservaDAO.existsSolapamiento(eq(100L), any(Date.class), any(Date.class))).thenReturn(true);

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si NO directa: estado PENDIENTE, guarda y notifica al propietario")
    void guardarReserva_ok_noDirecta_pendiente_yNotifica() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), false));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        when(reservaDAO.existsSolapamiento(eq(100L), any(Date.class), any(Date.class))).thenReturn(false);

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        ArgumentCaptor<Reserva> cap = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(cap.capture());

        Reserva guardada = cap.getValue();
        assertThat(guardada.getInquilino()).isSameAs(inq);
        assertThat(guardada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);

        verify(gestorNotificaciones, times(1)).nuevaReserva(prop, inm);
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si directa: estado ACEPTADA, guarda y notifica al propietario")
    void guardarReserva_ok_directa_aceptada_yNotifica() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), true));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        when(reservaDAO.existsSolapamiento(eq(100L), any(Date.class), any(Date.class))).thenReturn(false);

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        ArgumentCaptor<Reserva> cap = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(cap.capture());

        Reserva guardada = cap.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoReserva.ACEPTADA);

        verify(gestorNotificaciones, times(1)).nuevaReserva(prop, inm);
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> si inmueble o propietario null, guarda pero NO notifica")
    void guardarReserva_ok_sinPropietario_noNotifica() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = new Inmueble();
        inm.setId(100L);
        inm.setTipo("Vivienda");
        inm.setDireccion("Calle X");
        inm.setCiudad("Talavera");
        inm.setPrecioPorNoche(10.0);
        inm.setPropietario(null);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), true));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        when(reservaDAO.existsSolapamiento(eq(100L), any(Date.class), any(Date.class))).thenReturn(false);

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, times(1)).save(any(Reserva.class));
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("GET /misReservas/aceptar/{id} -> si existe, pone ACEPTADA, guarda y notifica reservaConfirmada")
    void aceptarReserva_ok() throws Exception {
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(100L, prop);

        Reserva reserva = reservaBasica(1L, inq, inm);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        mockMvc.perform(get("/misReservas/aceptar/1")
                        .sessionAttr("usuario", prop))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.ACEPTADA);
        verify(reservaDAO, times(1)).save(reserva);
        verify(gestorNotificaciones, times(1)).reservaConfirmada(inq, inm);
    }

    @Test
    @DisplayName("GET /misReservas/aceptar/{id} -> si no existe redirige /misReservas/propietario")
    void aceptarReserva_noExiste_redirect() throws Exception {
        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/misReservas/aceptar/1")
                        .sessionAttr("usuario", propietarioMinimo(20L, "Prop")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).reservaConfirmada(any(), any());
    }

    @Test
    @DisplayName("GET /misReservas/rechazar/{id} -> pone RECHAZADA, pagado=false, guarda y notifica reservaRechazada")
    void rechazarReserva_ok() throws Exception {
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(100L, prop);

        Reserva reserva = reservaBasica(1L, inq, inm);
        reserva.setPagado(true);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        mockMvc.perform(get("/misReservas/rechazar/1")
                        .sessionAttr("usuario", prop))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);
        assertThat(reserva.isPagado()).isFalse();
        verify(reservaDAO, times(1)).save(reserva);
        verify(gestorNotificaciones, times(1)).reservaRechazada(inq, inm);
    }

    @Test
    @DisplayName("GET /misReservas/rechazar/{id} -> si no existe redirige /misReservas/propietario")
    void rechazarReserva_noExiste_redirect() throws Exception {
        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/misReservas/rechazar/1")
                        .sessionAttr("usuario", propietarioMinimo(20L, "Prop")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).reservaRechazada(any(), any());
    }

    @Test
    @DisplayName("GET /misReservas/eliminar/{id} -> si reserva null: inquilino -> /misReservas")
    void eliminarReserva_noExiste_inquilino_redirect() throws Exception {
        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/misReservas/eliminar/1")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).delete(any());
    }

    @Test
    @DisplayName("GET /misReservas/eliminar/{id} -> si reserva null: propietario -> /misReservas/propietario")
    void eliminarReserva_noExiste_propietario_redirect() throws Exception {
        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/misReservas/eliminar/1")
                        .sessionAttr("usuario", propietarioMinimo(20L, "Prop")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        verify(reservaDAO, never()).delete(any());
    }

    @Test
    @DisplayName("GET /misReservas/eliminar/{id} -> inquilino: notifica cancelación al propietario, borra y /misReservas")
    void eliminarReserva_inquilino_ok() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        Reserva reserva = reservaBasica(1L, inq, inm);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        mockMvc.perform(get("/misReservas/eliminar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(gestorNotificaciones, times(1)).reservaCanceladaPorInquilino(prop, inm, inq);
        verify(reservaDAO, times(1)).delete(reserva);
    }

    @Test
    @DisplayName("GET /misReservas/eliminar/{id} -> propietario: notifica cancelación al inquilino, borra y /misReservas/propietario")
    void eliminarReserva_propietario_ok() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        Reserva reserva = reservaBasica(1L, inq, inm);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        mockMvc.perform(get("/misReservas/eliminar/1")
                        .sessionAttr("usuario", prop))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        verify(gestorNotificaciones, times(1)).reservaCanceladaPorPropietario(inq, inm, prop);
        verify(reservaDAO, times(1)).delete(reserva);
    }

   

    private static Date d(int year, int month, int day) {
        return java.sql.Date.valueOf(java.time.LocalDate.of(year, month, day));
    }

    private static Inquilino inquilinoMinimo(Long id, String nombre) {
        Inquilino i = new Inquilino();
        i.setId(id);
        i.setNombre(nombre);
        return i;
    }

    private static Propietario propietarioMinimo(Long id, String nombre) {
        Propietario p = new Propietario();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }

    private static Inmueble inmuebleBasico(Long id, Propietario propietario) {
        Inmueble inm = new Inmueble();
        inm.setId(id);
        inm.setTipo("Vivienda");
        inm.setDireccion("Calle Falsa 123");
        inm.setCiudad("Talavera");
        inm.setPrecioPorNoche(25.0);
        inm.setPropietario(propietario);
        return inm;
    }

    private static Disponibilidad disponibilidadRango(Date ini, Date fin, boolean directa) {
        Disponibilidad d = new Disponibilidad();
        d.setFechaInicio(ini);
        d.setFechaFin(fin);
        d.setDirecta(directa);
        return d;
    }

    private static Reserva reservaBasica(Long id, Inquilino inq, Inmueble inm) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setInquilino(inq);
        r.setInmueble(inm);
        r.setFechaInicio(d(2026, 5, 10));
        r.setFechaFin(d(2026, 5, 12));
        r.setPagado(false);
        r.setEstado(EstadoReserva.PENDIENTE);
        return r;
    }

    

    @Test
    @DisplayName("procesarDevolucion(null) -> return inmediato (cubre rama reserva==null)")
    void procesarDevolucion_null_cubreBranch() throws Exception {
        Method m = GestorReservas.class.getDeclaredMethod("procesarDevolucion", Reserva.class);
        m.setAccessible(true);

        
        assertDoesNotThrow(() -> m.invoke(gestorReservas, new Object[]{null}));
    }

    @Test
    @DisplayName("GET /misReservas/aceptar/{id} -> si notificación falla entra en catch y NO rompe")
    void aceptarReserva_notificacionFalla_cubreCatch() throws Exception {
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(100L, prop);

        Reserva reserva = reservaBasica(1L, inq, inm);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        doThrow(new RuntimeException("boom"))
                .when(gestorNotificaciones).reservaConfirmada(any(Inquilino.class), any(Inmueble.class));

        mockMvc.perform(get("/misReservas/aceptar/1")
                        .sessionAttr("usuario", prop))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        verify(reservaDAO, times(1)).save(reserva);
        
        verify(gestorNotificaciones, times(1)).reservaConfirmada(inq, inm);
    }

    @Test
    @DisplayName("GET /misReservas/rechazar/{id} -> si notificación falla entra en catch y deja pagado=false (cubre catch + procesarDevolucion)")
    void rechazarReserva_notificacionFalla_cubreCatch_yDevolucion() throws Exception {
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Inmueble inm = inmuebleBasico(100L, prop);

        Reserva reserva = reservaBasica(1L, inq, inm);
        reserva.setPagado(true);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        doThrow(new RuntimeException("boom"))
                .when(gestorNotificaciones).reservaRechazada(any(Inquilino.class), any(Inmueble.class));

        mockMvc.perform(get("/misReservas/rechazar/1")
                        .sessionAttr("usuario", prop))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas/propietario"));

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);
        assertThat(reserva.isPagado()).isFalse();
        verify(reservaDAO, times(1)).save(reserva);
       
        verify(gestorNotificaciones, times(1)).reservaRechazada(inq, inm);
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> notificación falla entra en catch y NO rompe (cubre catch)")
    void guardarReserva_notificacionFalla_cubreCatch() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), true));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        when(reservaDAO.existsSolapamiento(eq(100L), any(Date.class), any(Date.class))).thenReturn(false);

        
        doThrow(new RuntimeException("boom"))
                .when(gestorNotificaciones).nuevaReserva(prop, inm);

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, times(1)).save(any(Reserva.class));
        verify(gestorNotificaciones, times(1)).nuevaReserva(prop, inm);
    }

    @Test
    @DisplayName("GET /misReservas/eliminar/{id} -> si notificación lanza excepción entra en catch y redirige /misReservas (cubre catch)")
    void eliminarReserva_catch_notificacionFalla() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        Reserva reserva = reservaBasica(1L, inq, inm);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        
        doThrow(new RuntimeException("boom"))
                .when(gestorNotificaciones)
                .reservaCanceladaPorInquilino(prop, inm, inq);

        mockMvc.perform(get("/misReservas/eliminar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).delete(any());
    }

    @Test
    @DisplayName("GET /misReservas/eliminar/{id} -> usuario genérico (no Inquilino/Propietario) redirige /misReservas")
    void eliminarReserva_usuarioGenerico_redirectFinal() throws Exception {
        Usuario u = mock(Usuario.class);

        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(100L, prop);
        Reserva reserva = reservaBasica(1L, inq, inm);

        when(reservaDAO.findById(1L)).thenReturn(java.util.Optional.of(reserva));

        mockMvc.perform(get("/misReservas/eliminar/1")
                        .sessionAttr("usuario", u))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(gestorNotificaciones, never()).reservaCanceladaPorInquilino(any(), any(), any());
        verify(gestorNotificaciones, never()).reservaCanceladaPorPropietario(any(), any(), any());
        verify(reservaDAO, never()).delete(any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> inmueble con id null redirige /misReservas (cubre rama inmueble.getId()==null)")
    void guardarReserva_inmuebleIdNull_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");

        Inmueble inm = new Inmueble();
        inm.setId(null);

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> fechaInicio null redirige (cubre reserva.getFechaInicio()==null)")
    void guardarReserva_fechaInicioNull_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");

        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), true));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(null);
        reserva.setFechaFin(d(2026, 5, 12));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> fechaFin null redirige (cubre reserva.getFechaFin()==null)")
    void guardarReserva_fechaFinNull_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");

        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 1, 1), d(2026, 12, 31), true));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(null);

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> disponibilidad existe pero fechaInicio disp null redirige (cubre getDisponibilidad().getFechaInicio()==null)")
    void guardarReserva_disponibilidadFechaInicioNull_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");

        Inmueble inm = inmuebleBasico(100L, prop);

        Disponibilidad disp = new Disponibilidad();
        disp.setFechaInicio(null);
        disp.setFechaFin(d(2026, 12, 31));
        disp.setDirecta(true);
        inm.setDisponibilidad(disp);

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> disponibilidad existe pero fechaFin disp null redirige (cubre getDisponibilidad().getFechaFin()==null)")
    void guardarReserva_disponibilidadFechaFinNull_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");

        Inmueble inm = inmuebleBasico(100L, prop);

        Disponibilidad disp = new Disponibilidad();
        disp.setFechaInicio(d(2026, 1, 1));
        disp.setFechaFin(null);
        disp.setDirecta(true);
        inm.setDisponibilidad(disp);

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 5, 10));
        reserva.setFechaFin(d(2026, 5, 12));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }

    @Test
    @DisplayName("POST /misReservas/guardar -> fin después de dispFin redirige (cubre OR: reserva.getFechaFin().after(dispFin))")
    void guardarReserva_finDespuesDeDisponibilidad_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");

        Inmueble inm = inmuebleBasico(100L, prop);
        inm.setDisponibilidad(disponibilidadRango(d(2026, 6, 1), d(2026, 6, 30), false));

        Reserva reserva = new Reserva();
        reserva.setInmueble(inm);
        reserva.setFechaInicio(d(2026, 6, 10));
        reserva.setFechaFin(d(2026, 7, 2));

        mockMvc.perform(post("/misReservas/guardar")
                        .sessionAttr("usuario", inq)
                        .flashAttr("reserva", reserva))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(reservaDAO, never()).save(any());
        verify(gestorNotificaciones, never()).nuevaReserva(any(), any());
    }
}
