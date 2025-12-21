package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.persistence.NotificacionDAO;
import jakarta.servlet.http.HttpSession;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GestorNotificaciones.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class GestorNotificacionesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificacionDAO notificacionDAO;

   

    private static Inquilino inquilinoConId(long id) {
        Inquilino i = new Inquilino();
        i.setId(id);
        i.setNombre("Inq");
        return i;
    }

    private static Propietario propietarioConId(long id) {
        Propietario p = new Propietario();
        p.setId(id);
        p.setNombre("Prop");
        return p;
    }

    private static Inmueble inmuebleMinimo(long id, String direccion) {
        Inmueble inm = new Inmueble();
        inm.setId(id);
        inm.setDireccion(direccion);
        inm.setCiudad("Ciudad");
        inm.setTipo("Vivienda");
        inm.setPrecioPorNoche(50.0);
        inm.setPropietario(propietarioConId(99L));
        return inm;
    }

    private static Reserva reservaMinima(String direccion) {
        Reserva r = new Reserva();
        Inmueble inm = new Inmueble();
        inm.setId(77L);
        inm.setDireccion(direccion);
        inm.setCiudad("Ciudad");
        inm.setTipo("Vivienda");
        inm.setPrecioPorNoche(50.0);
        inm.setPropietario(propietarioConId(99L));
        r.setInmueble(inm);
       
        return r;
    }


    private static Notificacion noti(Long id, Usuario destino, boolean leida) {
        Notificacion n = new Notificacion();
        n.setId(id);
        n.setUsuarioDestino(destino);
        n.setLeido(leida);
        n.setTipo("TEST");
        n.setMensaje("msg");
        n.setFecha(new Date());
        return n;
    }

    
    @Test
    @DisplayName("GET /notificaciones sin usuario en sesión -> redirect /login")
    void verNotificaciones_sinSesion_redirectLogin() throws Exception {
        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("GET /notificaciones con Inquilino -> vista notificaciones + model correcto")
    void verNotificaciones_inquilino_ok() throws Exception {
        Inquilino inq = inquilinoConId(1L);
        List<Notificacion> lista = List.of(noti(10L, inq, false), noti(11L, inq, true));

        when(notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(any(Usuario.class))).thenReturn(lista);

        mockMvc.perform(get("/notificaciones").sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(view().name("notificaciones"))
                .andExpect(model().attributeExists("notificaciones"))
                .andExpect(model().attribute("esInquilino", true))
                .andExpect(model().attribute("esPropietario", false));

        verify(notificacionDAO, times(1)).findByUsuarioDestinoOrderByFechaDesc(inq);
    }

    @Test
    @DisplayName("GET /notificaciones con Propietario -> vista notificaciones + flags correcto")
    void verNotificaciones_propietario_ok() throws Exception {
        Propietario prop = propietarioConId(2L);

        when(notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(any(Usuario.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones").sessionAttr("usuario", prop))
                .andExpect(status().isOk())
                .andExpect(view().name("notificaciones"))
                .andExpect(model().attributeExists("notificaciones"))
                .andExpect(model().attribute("esInquilino", false))
                .andExpect(model().attribute("esPropietario", true));

        verify(notificacionDAO, times(1)).findByUsuarioDestinoOrderByFechaDesc(prop);
    }



    @Test
    @DisplayName("GET /notificaciones/leida/{id} sin sesión -> redirect /login")
    void marcarLeida_sinSesion_redirectLogin() throws Exception {
        mockMvc.perform(get("/notificaciones/leida/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("GET /notificaciones/leida/{id} si no existe -> redirect /notificaciones y no guarda")
    void marcarLeida_noExiste_noGuarda() throws Exception {
        Inquilino inq = inquilinoConId(1L);
        when(notificacionDAO.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/notificaciones/leida/10").sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notificaciones"));

        verify(notificacionDAO, times(1)).findById(10L);
        verify(notificacionDAO, never()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("GET /notificaciones/leida/{id} si no es del usuario -> redirect /notificaciones y no guarda")
    void marcarLeida_noEsDelUsuario_noGuarda() throws Exception {
        Inquilino inq = inquilinoConId(1L);
        Inquilino otro = inquilinoConId(999L);

        Notificacion n = noti(10L, otro, false);
        when(notificacionDAO.findById(10L)).thenReturn(Optional.of(n));

        mockMvc.perform(get("/notificaciones/leida/10").sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notificaciones"));

        verify(notificacionDAO, times(1)).findById(10L);
        verify(notificacionDAO, never()).save(any(Notificacion.class));
        assertThat(n.isLeido()).isFalse();
    }

    @Test
    @DisplayName("GET /notificaciones/leida/{id} ok -> marca leido=true y guarda")
    void marcarLeida_ok_guarda() throws Exception {
        Inquilino inq = inquilinoConId(1L);
        Notificacion n = noti(10L, inq, false);

        when(notificacionDAO.findById(10L)).thenReturn(Optional.of(n));

        mockMvc.perform(get("/notificaciones/leida/10").sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notificaciones"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionDAO, times(1)).save(captor.capture());

        Notificacion guardada = captor.getValue();
        assertThat(guardada.isLeido()).isTrue();
    }

  

    @Test
    @DisplayName("GET /notificaciones/limpiar sin sesión -> redirect /login")
    void limpiarLeidas_sinSesion_redirectLogin() throws Exception {
        mockMvc.perform(get("/notificaciones/limpiar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("GET /notificaciones/limpiar con sesión -> borra solo leídas")
    void limpiarLeidas_ok_borraSoloLeidas() throws Exception {
        Inquilino inq = inquilinoConId(1L);

        Notificacion leida1 = noti(1L, inq, true);
        Notificacion noLeida = noti(2L, inq, false);
        Notificacion leida2 = noti(3L, inq, true);

        when(notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(inq))
                .thenReturn(List.of(leida1, noLeida, leida2));

        mockMvc.perform(get("/notificaciones/limpiar").sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notificaciones"));

        verify(notificacionDAO, times(1)).findByUsuarioDestinoOrderByFechaDesc(inq);
        verify(notificacionDAO, times(1)).delete(leida1);
        verify(notificacionDAO, never()).delete(noLeida);
        verify(notificacionDAO, times(1)).delete(leida2);
    }


    @Test
    @DisplayName("GET /notificaciones/noLeidas sin sesión -> devuelve 0")
    void contarNoLeidas_sinSesion_devuelve0() throws Exception {
        mockMvc.perform(get("/notificaciones/noLeidas"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("GET /notificaciones/noLeidas con sesión -> cuenta correctamente")
    void contarNoLeidas_ok_cuenta() throws Exception {
        Inquilino inq = inquilinoConId(1L);

        Notificacion n1 = noti(1L, inq, false);
        Notificacion n2 = noti(2L, inq, true);
        Notificacion n3 = noti(3L, inq, false);

        when(notificacionDAO.findByUsuarioDestinoOrderByFechaDesc(inq))
                .thenReturn(List.of(n1, n2, n3));

        mockMvc.perform(get("/notificaciones/noLeidas").sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        verify(notificacionDAO, times(1)).findByUsuarioDestinoOrderByFechaDesc(inq);
    }

  

    @Test
    @DisplayName("enviar(): destino null o mensaje vacío -> no guarda")
    void enviar_validaciones_noGuarda() {
        GestorNotificaciones gn = new GestorNotificaciones();
       
        try {
            var f = GestorNotificaciones.class.getDeclaredField("notificacionDAO");
            f.setAccessible(true);
            f.set(gn, notificacionDAO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        gn.enviar(null, "X", "hola");
        gn.enviar(inquilinoConId(1L), "X", "");
        gn.enviar(inquilinoConId(1L), "X", "   ");

        verify(notificacionDAO, never()).save(any(Notificacion.class));
    }

    @Test
    @DisplayName("enviar(): crea notificación con leido=false, fecha no null y guarda")
    void enviar_ok_creaYGuarda() {
        GestorNotificaciones gn = new GestorNotificaciones();
        try {
            var f = GestorNotificaciones.class.getDeclaredField("notificacionDAO");
            f.setAccessible(true);
            f.set(gn, notificacionDAO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Inquilino inq = inquilinoConId(1L);
        gn.enviar(inq, "TIPO_TEST", "Mensaje de prueba");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionDAO, times(1)).save(captor.capture());

        Notificacion guardada = captor.getValue();
        assertThat(guardada.getUsuarioDestino()).isEqualTo(inq);
        assertThat(guardada.getTipo()).isEqualTo("TIPO_TEST");
        assertThat(guardada.getMensaje()).isEqualTo("Mensaje de prueba");
        assertThat(guardada.isLeido()).isFalse();
        assertThat(guardada.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("Eventos de reservas/inmuebles/pagos llaman a enviar() con tipo correcto")
    void eventos_llamanEnviar() {
        GestorNotificaciones gn = spy(new GestorNotificaciones());

       
        doNothing().when(gn).enviar(any(Usuario.class), anyString(), anyString());

        Propietario prop = propietarioConId(10L);
        Inquilino inq = inquilinoConId(20L);
        Inmueble inm = inmuebleMinimo(1L, "Calle Falsa 123");
        Reserva res = reservaMinima("Calle Falsa 123");

        gn.nuevaReserva(prop, inm);
        verify(gn).enviar(eq(prop), eq("RESERVA_NUEVA"), contains("Calle Falsa 123"));

        gn.reservaConfirmada(inq, inm);
        verify(gn).enviar(eq(inq), eq("RESERVA_CONFIRMADA"), contains("Calle Falsa 123"));

        gn.reservaRechazada(inq, inm);
        verify(gn).enviar(eq(inq), eq("RESERVA_RECHAZADA"), contains("Calle Falsa 123"));

        gn.reservaCanceladaPorInquilino(prop, inm, inq);
        verify(gn).enviar(eq(prop), eq("RESERVA_CANCELADA_INQUILINO"), contains("Calle Falsa 123"));

        gn.reservaCanceladaPorPropietario(inq, inm, prop);
        verify(gn).enviar(eq(inq), eq("RESERVA_CANCELADA_PROPIETARIO"), contains("Calle Falsa 123"));

        gn.reservaProxima(inq, inm);
        verify(gn).enviar(eq(inq), eq("RESERVA_PROXIMA"), contains("Calle Falsa 123"));

        gn.inmueblePublicado(prop, inm);
        verify(gn).enviar(eq(prop), eq("INMUEBLE_PUBLICADO"), contains("Calle Falsa 123"));

        gn.inmuebleActualizado(prop, inm);
        verify(gn).enviar(eq(prop), eq("INMUEBLE_ACTUALIZADO"), contains("Calle Falsa 123"));

        gn.pagoConfirmado(inq, res);
        verify(gn).enviar(eq(inq), eq("PAGO_CONFIRMADO"), contains("Calle Falsa 123"));

        gn.pagoRecibido(prop, res);
        verify(gn).enviar(eq(prop), eq("PAGO_RECIBIDO"), contains("Calle Falsa 123"));
    }
}
