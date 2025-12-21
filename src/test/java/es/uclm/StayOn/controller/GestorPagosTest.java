package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.persistence.PagoDAO;
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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GestorPagos.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class GestorPagosTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservaDAO reservaDAO;

    @MockBean
    private PagoDAO pagoDAO;

    @MockBean
    private GestorNotificaciones gestorNotificaciones;

   

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
        inm.setDireccion("Calle X 123");
        inm.setCiudad("Madrid");
        inm.setTipo("Vivienda");
        inm.setPrecioPorNoche(100.0);
        inm.setPropietario(propietario);
        return inm;
    }

    private static Disponibilidad disponibilidadConPolitica(PoliticaCancelacion politica) {
        Disponibilidad d = new Disponibilidad();
        d.setPoliticaCancelacion(politica);
        return d;
    }

    private static Reserva reservaBasica(Long id, Inquilino inq, Inmueble inm) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setInquilino(inq);
        r.setInmueble(inm);
        r.setFechaInicio(new Date());
        r.setFechaFin(new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000));
        r.setPrecioTotal(100.0);
        return r;
    }

 
    @Test
    @DisplayName("GET /pagos -> muestra historial de pagos para inquilino en sesión")
    void verPagos_ok() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");

        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r1 = reservaBasica(1L, inq, inm);
        Reserva r2 = reservaBasica(2L, inq, inm);

        Pago p1 = new Pago();
        p1.setReserva(r1);

        Pago p2 = new Pago();
        p2.setReserva(r2);

        when(pagoDAO.findByReserva_Inquilino(inq)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/pagos")
                        .sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(view().name("pagos"))
                .andExpect(model().attributeExists("pagos"))
                .andExpect(model().attributeExists("inquilino"));

        verify(pagoDAO, times(1)).findByReserva_Inquilino(inq);
    }




    @Test
    @DisplayName("GET /pagos/pagar/{id} -> si reserva no existe redirige a /misReservas")
    void mostrarFormularioPago_reservaNoExiste_redirect() throws Exception {
        when(reservaDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/pagos/pagar/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));
    }

    @Test
    @DisplayName("GET /pagos/pagar/{id} -> si reserva ya pagada redirige a /misReservas")
    void mostrarFormularioPago_reservaPagada_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.ACEPTADA);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/pagar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));
    }

    @Test
    @DisplayName("GET /pagos/pagar/{id} -> si estado != ACEPTADA redirige a /misReservas")
    void mostrarFormularioPago_estadoNoAceptada_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(false);
        r.setEstado(EstadoReserva.PENDIENTE);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/pagar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));
    }

    @Test
    @DisplayName("GET /pagos/pagar/{id} -> ok devuelve formularioPago con pago/reserva/total")
    void mostrarFormularioPago_ok() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(false);
        r.setEstado(EstadoReserva.ACEPTADA);
        r.setPrecioTotal(250.0);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/pagar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioPago"))
                .andExpect(model().attributeExists("pago"))
                .andExpect(model().attributeExists("reserva"))
                .andExpect(model().attribute("total", 250.0));
    }

  

    @Test
    @DisplayName("POST /pagos/procesarPago -> si reserva no existe redirige a /misReservas")
    void procesarPago_reservaNoExiste_redirect() throws Exception {
        when(reservaDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/pagos/procesarPago")
                        .param("reservaId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(pagoDAO, never()).save(any());
        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /pagos/procesarPago -> ok guarda pago, marca reserva pagada y notifica")
    void procesarPago_ok_guardaYNotifica() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(false);
        r.setEstado(EstadoReserva.ACEPTADA);
        r.setPrecioTotal(300.0);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/pagos/procesarPago")
                        .param("reservaId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

     
        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoDAO, times(1)).save(pagoCaptor.capture());
        Pago pagoGuardado = pagoCaptor.getValue();

        assertThat(pagoGuardado.getReserva()).isNotNull();
        assertThat(pagoGuardado.getReferencia()).isNotBlank();
        assertThat(pagoGuardado.getReferencia().length()).isEqualTo(10);
        assertThat(pagoGuardado.isReembolsado()).isFalse();
        assertThat(pagoGuardado.getImporteReembolsado()).isEqualTo(0.0);
        assertThat(pagoGuardado.getFechaReembolso()).isNull();

      
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(reservaCaptor.capture());
        Reserva reservaGuardada = reservaCaptor.getValue();

        assertThat(reservaGuardada.isPagado()).isTrue();
        assertThat(reservaGuardada.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reservaGuardada.getPago()).isNotNull();

        
        verify(gestorNotificaciones, times(1)).pagoConfirmado(eq(inq), any(Reserva.class));
        verify(gestorNotificaciones, times(1)).pagoRecibido(eq(prop), any(Reserva.class));
    }

    

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> si reserva no existe redirect")
    void cancelar_reservaNoExiste_redirect() throws Exception {
        when(reservaDAO.findById(50L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/pagos/cancelar/50")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> si reserva no es del inquilino logueado redirect")
    void cancelar_reservaDeOtroInquilino_redirect() throws Exception {
        Inquilino logueado = inquilinoMinimo(10L, "Ana");
        Inquilino otro = inquilinoMinimo(11L, "Pepe");

        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, otro, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", logueado))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(pagoDAO, never()).save(any());
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> si no está pagada o no está CONFIRMADA redirect")
    void cancelar_noPagadaONoConfirmada_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(false);
        r.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(pagoDAO, never()).save(any());
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> ok calcula reembolso según política, actualiza pago y reserva y notifica")
    void cancelar_ok_conReembolso_50porciento() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        
        Disponibilidad disp = disponibilidadConPolitica(PoliticaCancelacion.REEMBOLSABLE_50_PER);
        inm.setDisponibilidad(disp);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPrecioTotal(200.0);
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);

        Pago pago = new Pago();
        pago.setReserva(r);
        r.setPago(pago);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

       
        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoDAO, times(1)).save(pagoCaptor.capture());
        Pago pagoActualizado = pagoCaptor.getValue();

        assertThat(pagoActualizado.isReembolsado()).isTrue();
        assertThat(pagoActualizado.getImporteReembolsado()).isEqualTo(200.0 * 0.5);
        assertThat(pagoActualizado.getFechaReembolso()).isNotNull();

       
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(reservaCaptor.capture());
        Reserva reservaActualizada = reservaCaptor.getValue();

        assertThat(reservaActualizada.isPagado()).isFalse();
        assertThat(reservaActualizada.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);

       
        verify(gestorNotificaciones, times(1)).reservaCanceladaPorInquilino(eq(prop), eq(inm), eq(inq));
    }
    @Test
    @DisplayName("POST /pagos/procesarPago -> si notificación falla entra en catch y NO rompe")
    void procesarPago_notificacionFalla_cubreCatch() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(false);
        r.setEstado(EstadoReserva.ACEPTADA);
        r.setPrecioTotal(123.0);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        
        doThrow(new RuntimeException("boom")).when(gestorNotificaciones)
                .pagoConfirmado(any(Inquilino.class), any(Reserva.class));

        mockMvc.perform(post("/pagos/procesarPago")
                        .param("reservaId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

       
        verify(pagoDAO, times(1)).save(any(Pago.class));
        verify(reservaDAO, times(1)).save(any(Reserva.class));

        
        verify(gestorNotificaciones, times(1)).pagoConfirmado(eq(inq), any(Reserva.class));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> reserva confirmada SIN pago: entra rama pago==null, actualiza reserva y notifica")
    void cancelar_ok_pagoNull_cubreWarning_y_ifPagoNull() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        
        Disponibilidad disp = new Disponibilidad();
        disp.setPoliticaCancelacion(PoliticaCancelacion.REEMBOLSABLE_50_PER);
        inm.setDisponibilidad(disp);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setPrecioTotal(200.0);
        r.setPago(null); 

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(pagoDAO, never()).save(any()); 
        verify(reservaDAO, times(1)).save(any(Reserva.class));
        verify(gestorNotificaciones, times(1))
                .reservaCanceladaPorInquilino(eq(prop), eq(inm), eq(inq));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> sin politica (inmueble null) => porcentaje 0 y reembolso 0: reembolsado=false, fechaReembolso=null")
    void cancelar_ok_sinPolitica_reembolsoCero_cubrePorcentaje0_y_fechaNull() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");

        Reserva r = new Reserva();
        r.setId(1L);
        r.setInquilino(inq);
        r.setInmueble(null);            
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setPrecioTotal(100.0);

        Pago pago = new Pago();
        pago.setReserva(r);
        r.setPago(pago);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        ArgumentCaptor<Pago> capPago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoDAO).save(capPago.capture());
        Pago actualizado = capPago.getValue();

        assertThat(actualizado.isReembolsado()).isFalse();     
        assertThat(actualizado.getImporteReembolsado()).isEqualTo(0.0);
        assertThat(actualizado.getFechaReembolso()).isNull();  

        
        ArgumentCaptor<Reserva> capRes = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO).save(capRes.capture());
        Reserva resAct = capRes.getValue();

        assertThat(resAct.isPagado()).isFalse();
        assertThat(resAct.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);

        
        verify(gestorNotificaciones, never()).reservaCanceladaPorInquilino(any(), any(), any());
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> total null => usa 0.0 (ternario), y si notificación falla entra en catch")
    void cancelar_totalNull_y_notificacionFalla_cubreTotalTernario_yCatch() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

       
        Disponibilidad disp = new Disponibilidad();
        disp.setPoliticaCancelacion(PoliticaCancelacion.REEMBOLSABLE_50_PER);
        inm.setDisponibilidad(disp);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setPrecioTotal(null); 

        Pago pago = new Pago();
        pago.setReserva(r);
        r.setPago(pago);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        
        doThrow(new RuntimeException("boom")).when(gestorNotificaciones)
                .reservaCanceladaPorInquilino(any(), any(), any());

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        
        ArgumentCaptor<Pago> capPago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoDAO).save(capPago.capture());
        Pago pAct = capPago.getValue();
        assertThat(pAct.getImporteReembolsado()).isEqualTo(0.0);
        assertThat(pAct.isReembolsado()).isFalse();
        assertThat(pAct.getFechaReembolso()).isNull();

       
        verify(reservaDAO, times(1)).save(any(Reserva.class));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> si reserva tiene inquilino null => redirect (cubre rama reserva.getInquilino()==null)")
    void cancelar_reservaConInquilinoNull_redirect() throws Exception {
        Inquilino logueado = inquilinoMinimo(10L, "Ana");

        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, null, inm);
        r.setInquilino(null); 
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", logueado))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(pagoDAO, never()).save(any());
        verify(reservaDAO, never()).save(any(Reserva.class));
        verify(gestorNotificaciones, never()).reservaCanceladaPorInquilino(any(), any(), any());
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> pagada=true pero estado != CONFIRMADA => redirect (cubre OR por lado estado)")
    void cancelar_pagadaPeroNoConfirmada_redirect() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");

        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.ACEPTADA); 

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        verify(pagoDAO, never()).save(any());
        verify(reservaDAO, never()).save(any(Reserva.class));
        verify(gestorNotificaciones, never()).reservaCanceladaPorInquilino(any(), any(), any());
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> política NULL (disp existe pero politica null) y pago!=null: reembolso 0 => guarda pago con reembolsado=false")
    void cancelar_politicaNull_pagoGuardado_reembolsoCero() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

       
        Disponibilidad disp = new Disponibilidad();
        disp.setPoliticaCancelacion(null); 
        inm.setDisponibilidad(disp);

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setPrecioTotal(200.0);

        Pago pago = new Pago();
        pago.setReserva(r);
        r.setPago(pago);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        ArgumentCaptor<Pago> capPago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoDAO, times(1)).save(capPago.capture());
        Pago pAct = capPago.getValue();

        
        assertThat(pAct.isReembolsado()).isFalse();
        assertThat(pAct.getImporteReembolsado()).isEqualTo(0.0);
        assertThat(pAct.getFechaReembolso()).isNull();

        ArgumentCaptor<Reserva> capRes = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(capRes.capture());
        Reserva rAct = capRes.getValue();

        assertThat(rAct.isPagado()).isFalse();
        assertThat(rAct.getEstado()).isEqualTo(EstadoReserva.RECHAZADA);

        verify(gestorNotificaciones, times(1))
                .reservaCanceladaPorInquilino(eq(prop), eq(inm), eq(inq));
    }

    @Test
    @DisplayName("GET /pagos/cancelar/{id} -> disponibilidad NULL (cubre rama disponibilidad==null en if de política) con pago!=null")
    void cancelar_disponibilidadNull_pagoGuardado_reembolsoCero() throws Exception {
        Inquilino inq = inquilinoMinimo(10L, "Ana");
        Propietario prop = propietarioMinimo(20L, "Prop");
        Inmueble inm = inmuebleBasico(1L, prop);

        inm.setDisponibilidad(null); 

        Reserva r = reservaBasica(1L, inq, inm);
        r.setPagado(true);
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setPrecioTotal(120.0);

        Pago pago = new Pago();
        pago.setReserva(r);
        r.setPago(pago);

        when(reservaDAO.findById(1L)).thenReturn(Optional.of(r));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(get("/pagos/cancelar/1")
                        .sessionAttr("usuario", inq))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/misReservas"));

        ArgumentCaptor<Pago> capPago = ArgumentCaptor.forClass(Pago.class);
        verify(pagoDAO, times(1)).save(capPago.capture());
        Pago pAct = capPago.getValue();

        assertThat(pAct.isReembolsado()).isFalse();
        assertThat(pAct.getImporteReembolsado()).isEqualTo(0.0);
        assertThat(pAct.getFechaReembolso()).isNull();

        verify(gestorNotificaciones, times(1))
                .reservaCanceladaPorInquilino(eq(prop), eq(inm), eq(inq));
    }
    @Test
    void settersBasicos_y_id_y_metodo_ok() {
        Pago p = new Pago();

        p.setId(99L);
        p.setEmailPaypal("ana@paypal.com");
        p.setCvv("123");
        p.setFechaCaducidad("12/30");
        p.setNumeroTarjeta("4111111111111111");
        p.setMetodo(MetodoPago.TARJETA);

        assertThat(p.getId()).isEqualTo(99L);

        
        assertThat(p.getEmailPaypal()).isEqualTo("ana@paypal.com");
        assertThat(p.getCvv()).isEqualTo("123");
        assertThat(p.getFechaCaducidad()).isEqualTo("12/30");
        assertThat(p.getNumeroTarjeta()).isEqualTo("4111111111111111");
        assertThat(p.getMetodo()).isEqualTo(MetodoPago.TARJETA);
    }

}
