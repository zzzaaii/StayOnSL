package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.*;
import es.uclm.StayOn.entity.Reserva.EstadoReserva;
import es.uclm.StayOn.persistence.InmuebleDAO;
import es.uclm.StayOn.persistence.ReservaDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
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


import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;


@WebMvcTest(
 controllers = GestorBusquedas.class,
 excludeAutoConfiguration = {
     SecurityAutoConfiguration.class,
     SecurityFilterAutoConfiguration.class
 }
)
@AutoConfigureMockMvc(addFilters = false)
class GestorBusquedasTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InmuebleDAO inmuebleDAO;

    @MockBean
    private ReservaDAO reservaDAO;

    @MockBean
    private GestorNotificaciones gestorNotificaciones;

    private static Date d(String yyyyMmDd) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(yyyyMmDd);
    }

    private static Inmueble inmuebleBasico(Long id, String ciudad, String direccion, String tipo, double precio, boolean eliminado) {
        Inmueble i = new Inmueble();
        i.setId(id);
        i.setCiudad(ciudad);
        i.setDireccion(direccion);
        i.setTipo(tipo);
        i.setPrecioPorNoche(precio);
        i.setEliminado(eliminado);
        return i;
    }

    private static Disponibilidad disponibilidad(Date ini, Date fin, boolean directa, PoliticaCancelacion politica) {
        Disponibilidad d = new Disponibilidad();
        d.setFechaInicio(ini);
        d.setFechaFin(fin);
        d.setDirecta(directa);
        d.setPoliticaCancelacion(politica);
        return d;
    }

    private static Propietario propietarioMinimo(Long id, String nombre) {
        Propietario p = new Propietario();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }

    private static Inquilino inquilinoMinimo(Long id, String nombre) {
        Inquilino i = new Inquilino();
        i.setId(id);
        i.setNombre(nombre);
        return i;
    }

  
    @Test
    @DisplayName("GET /buscarInmuebles -> devuelve vista busqueda y model inicial")
    void mostrarPaginaBusqueda_ok() throws Exception {
        mockMvc.perform(get("/buscarInmuebles"))
                .andExpect(status().isOk())
                .andExpect(view().name("busqueda"))
                .andExpect(model().attributeExists("resultados"))
                .andExpect(model().attribute("busquedaRealizada", false));
    }

    @Test
    @DisplayName("POST /buscar filtra eliminados y aplica filtros de destino/tipo/precio/directa/politica/fechas")
    void buscarAlojamientos_filtraCorrectamente() throws Exception {
        Inmueble i1 = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        i1.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.REEMBOLSABLE_100_PER));

        Inmueble i2 = inmuebleBasico(2L, "Madrid", "Calle B", "Apartamento", 200.0, false);
        i2.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.REEMBOLSABLE_100_PER));

        Inmueble i3 = inmuebleBasico(3L, "Toledo", "Calle C", "Vivienda", 40.0, false);
        i3.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), false, PoliticaCancelacion.REEMBOLSABLE_100_PER));

        Inmueble i4 = inmuebleBasico(4L, "Madrid", "Calle D", "Vivienda", 60.0, true); 

        when(inmuebleDAO.findAll()).thenReturn(Arrays.asList(i1, i2, i3, i4));

        mockMvc.perform(post("/buscar")
                        .param("destino", "madrid")
                        .param("tipo", "Vivienda")
                        .param("precioMax", "100")
                        .param("directa", "true")
                        .param("fechaInicio", "2025-12-12")
                        .param("fechaFin", "2025-12-15")
                        .param("politica", "REEMBOLSABLE_100_PER"))
                .andExpect(status().isOk())
                .andExpect(view().name("busqueda"))
                .andExpect(model().attribute("busquedaRealizada", true))
                .andExpect(model().attributeExists("resultados"))
                .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
  }

    @Test
    @DisplayName("POST /buscar si fechas mal formateadas no rompe (simplemente no aplica filtro de fechas)")
    void buscarAlojamientos_fechasInvalidas_noRompe() throws Exception {
        Inmueble i1 = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        when(inmuebleDAO.findAll()).thenReturn(List.of(i1));

        mockMvc.perform(post("/buscar")
                        .param("fechaInicio", "no-es-fecha")
                        .param("fechaFin", "tampoco"))
                .andExpect(status().isOk())
                .andExpect(view().name("busqueda"))
                .andExpect(model().attribute("busquedaRealizada", true))
                .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
    }

   

    @Test
    @DisplayName("GET /reservar/{id} si no existe -> redirect a /buscarInmuebles")
    void mostrarFormularioReserva_inmuebleNoExiste_redirect() throws Exception {
        when(inmuebleDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/reservar/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buscarInmuebles"));
    }

    @Test
    @DisplayName("GET /reservar/{id} si está eliminado -> redirect a /buscarInmuebles")
    void mostrarFormularioReserva_inmuebleEliminado_redirect() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, true);
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/reservar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buscarInmuebles"));
    }

    @Test
    @DisplayName("GET /reservar/{id} ok -> vista formularioReserva con inmueble y reserva")
    void mostrarFormularioReserva_ok() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(get("/reservar/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioReserva"))
                .andExpect(model().attributeExists("inmueble"))
                .andExpect(model().attributeExists("reserva"));
    }

    

    @Test
    @DisplayName("POST /confirmarReserva si inmueble no existe -> redirect a /buscarInmuebles")
    void confirmarReserva_inmuebleNoExiste_redirect() throws Exception {
        when(inmuebleDAO.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "99")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buscarInmuebles"));
    }

    @Test
    @DisplayName("POST /confirmarReserva si inmueble eliminado -> redirect a /buscarInmuebles")
    void confirmarReserva_inmuebleEliminado_redirect() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, true);
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buscarInmuebles"));
    }

    @Test
    @DisplayName("POST /confirmarReserva validación 0: fechas obligatorias")
    void confirmarReserva_fechasObligatorias_error() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                       
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioReserva"))
                .andExpect(model().attributeExists("error"));
        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /confirmarReserva validación 1: fechaFin posterior a fechaInicio")
    void confirmarReserva_finPosterior_error() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .param("fechaInicio", "2025-12-15")
                        .param("fechaFin", "2025-12-15")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioReserva"))
                .andExpect(model().attributeExists("error"));
        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /confirmarReserva validación 2: inmueble sin rango de disponibilidad")
    void confirmarReserva_sinDisponibilidad_error() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        i.setDisponibilidad(null);
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .param("fechaInicio", "2025-12-12")
                        .param("fechaFin", "2025-12-15")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioReserva"))
                .andExpect(model().attributeExists("error"));
        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /confirmarReserva validación 2: fuera del rango disponible")
    void confirmarReserva_fueraDeRango_error() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        i.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .param("fechaInicio", "2025-12-09") 
                        .param("fechaFin", "2025-12-15")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioReserva"))
                .andExpect(model().attributeExists("error"));

        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /confirmarReserva validación 3: solape con otra reserva -> error")
    void confirmarReserva_solape_error() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
        i.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        when(reservaDAO.existsSolapamiento(eq(1L), any(Date.class), any(Date.class))).thenReturn(true);

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .param("fechaInicio", "2025-12-12")
                        .param("fechaFin", "2025-12-15")
                        .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
                .andExpect(status().isOk())
                .andExpect(view().name("formularioReserva"))
                .andExpect(model().attributeExists("error"));

        verify(reservaDAO, never()).save(any());
    }

    @Test
    @DisplayName("POST /confirmarReserva OK (directa=true): guarda reserva con campos aplicados y notifica")
    void confirmarReserva_ok_directa_guardaYNotifica() throws Exception {
        
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 80.0, false);
        i.setPropietario(propietarioMinimo(20L, "Prop"));
        i.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.REEMBOLSABLE_50_PER));
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        when(reservaDAO.existsSolapamiento(eq(1L), any(Date.class), any(Date.class))).thenReturn(false);

        Inquilino inq = inquilinoMinimo(10L, "Ana");

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .param("fechaInicio", "2025-12-12")
                        .param("fechaFin", "2025-12-15")
                        .sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(view().name("reservaConfirmada"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("esDirecta", true));

      
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(captor.capture());
        Reserva guardada = captor.getValue();

        assertThat(guardada.getInquilino()).isNotNull();
        assertThat(guardada.getInmueble()).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoReserva.ACEPTADA);
        assertThat(guardada.getPrecioPorNocheAplicado()).isEqualTo(80.0);
        assertThat(guardada.getPoliticaCancelacionAplicada()).isEqualTo(PoliticaCancelacion.REEMBOLSABLE_50_PER);
        assertThat(guardada.getReservaDirectaAplicada()).isTrue();

      
        assertThat(guardada.getPrecioTotal()).isEqualTo(80.0 * 3);

        
        verify(gestorNotificaciones, times(2)).enviar(any(Usuario.class), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /confirmarReserva OK (directa=false): estado PENDIENTE")
    void confirmarReserva_ok_noDirecta_estadoPendiente() throws Exception {
        Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 100.0, false);
        i.setPropietario(propietarioMinimo(20L, "Prop"));
        i.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), false, PoliticaCancelacion.NO_REEMBOLSABLE));
        when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

        when(reservaDAO.existsSolapamiento(eq(1L), any(Date.class), any(Date.class))).thenReturn(false);

        Inquilino inq = inquilinoMinimo(10L, "Ana");

        mockMvc.perform(post("/confirmarReserva")
                        .param("inmuebleId", "1")
                        .param("fechaInicio", "2025-12-12")
                        .param("fechaFin", "2025-12-13") 
                        .sessionAttr("usuario", inq))
                .andExpect(status().isOk())
                .andExpect(view().name("reservaConfirmada"))
                .andExpect(model().attribute("esDirecta", false));

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaDAO, times(1)).save(captor.capture());
        Reserva guardada = captor.getValue();

        assertThat(guardada.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(guardada.getPrecioTotal()).isEqualTo(100.0 * 1);
    }

 @Test
 @DisplayName("POST /buscar -> filtra inmuebles con tipo=null y ciudad=null (cubre OR del filtro inicial)")
 void buscar_filtra_tipoNull_y_ciudadNull() throws Exception {
     Inmueble ok = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);

     Inmueble fuera = new Inmueble();
     fuera.setId(2L);
     fuera.setEliminado(false);
     fuera.setTipo(null);
     fuera.setCiudad(null);
     fuera.setDireccion("X");
     fuera.setPrecioPorNoche(10.0);

     when(inmuebleDAO.findAll()).thenReturn(Arrays.asList(ok, fuera));

     mockMvc.perform(post("/buscar"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("busquedaRealizada", true))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
 }

 @Test
 @DisplayName("POST /buscar -> destino filtra por DIRECCION cuando ciudad no contiene (cubre OR ciudad||direccion)")
 void buscar_destino_filtraPorDireccion() throws Exception {
     Inmueble matchDir = inmuebleBasico(1L, "Toledo", "Avenida Sol 123", "Vivienda", 50.0, false);
     Inmueble noMatch = inmuebleBasico(2L, "Madrid", "Calle Luna 9", "Vivienda", 50.0, false);

     when(inmuebleDAO.findAll()).thenReturn(List.of(matchDir, noMatch));

     mockMvc.perform(post("/buscar")
                     .param("destino", "sol"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
 }

 @Test
 @DisplayName("POST /buscar -> tipo param dado: inmuebles con tipo=null se excluyen (cubre i.getTipo()!=null && equalsIgnoreCase)")
 void buscar_tipo_param_excluyeTipoNull() throws Exception {
     Inmueble conTipo = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);

     Inmueble tipoNull = inmuebleBasico(2L, "Madrid", "Calle B", null, 50.0, false);
     
     tipoNull.setTipo(null);

     when(inmuebleDAO.findAll()).thenReturn(List.of(conTipo, tipoNull));

     mockMvc.perform(post("/buscar")
                     .param("tipo", "Vivienda"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
 }

 @Test
 @DisplayName("POST /buscar -> precioMax <= 0 NO aplica filtro (cubre rama precioMax>0)")
 void buscar_precioMax_cero_noAplicaFiltro() throws Exception {
     Inmueble i1 = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);
     Inmueble i2 = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 150.0, false);

     when(inmuebleDAO.findAll()).thenReturn(List.of(i1, i2));

     mockMvc.perform(post("/buscar")
                     .param("precioMax", "0"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(2)));
 }

 @Test
 @DisplayName("POST /buscar -> precioMax > 0: inmuebles con precioPorNoche null se excluyen (cubre precioPorNoche!=null)")
 void buscar_precioMax_excluyePrecioNull() throws Exception {
     Inmueble ok = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);

     Inmueble precioNull = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 0.0, false);
     precioNull.setPrecioPorNoche(null);

     when(inmuebleDAO.findAll()).thenReturn(List.of(ok, precioNull));

     mockMvc.perform(post("/buscar")
                     .param("precioMax", "100"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
 }

 @Test
 @DisplayName("POST /buscar -> directa=true excluye disponibilidad null y directa=false (cubre rama directa)")
 void buscar_directa_true_filtraCorrecto() throws Exception {
     Inmueble directaOk = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);
     directaOk.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

     Inmueble noDirecta = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 50.0, false);
     noDirecta.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), false, PoliticaCancelacion.NO_REEMBOLSABLE));

     Inmueble dispNull = inmuebleBasico(3L, "Madrid", "C", "Vivienda", 50.0, false);
     dispNull.setDisponibilidad(null);

     when(inmuebleDAO.findAll()).thenReturn(List.of(directaOk, noDirecta, dispNull));

     mockMvc.perform(post("/buscar")
                     .param("directa", "true"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
 }

 @Test
 @DisplayName("POST /buscar -> politica!=null excluye disp null, politica null y politica distinta (cubre rama politica)")
 void buscar_politica_filtraCorrecto() throws Exception {
     Inmueble ok = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);
     ok.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.REEMBOLSABLE_100_PER));

     Inmueble dispNull = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 50.0, false);
     dispNull.setDisponibilidad(null);

     Inmueble politicaNull = inmuebleBasico(3L, "Madrid", "C", "Vivienda", 50.0, false);
     Disponibilidad dPolNull = new Disponibilidad();
     dPolNull.setFechaInicio(d("2025-12-10"));
     dPolNull.setFechaFin(d("2025-12-20"));
     dPolNull.setDirecta(true);
     dPolNull.setPoliticaCancelacion(null);
     politicaNull.setDisponibilidad(dPolNull);

     Inmueble politicaDistinta = inmuebleBasico(4L, "Madrid", "D", "Vivienda", 50.0, false);
     politicaDistinta.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

     when(inmuebleDAO.findAll()).thenReturn(List.of(ok, dispNull, politicaNull, politicaDistinta));

     mockMvc.perform(post("/buscar")
                     .param("politica", "REEMBOLSABLE_100_PER"))
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
 }

 @Test
 @DisplayName("POST /buscar -> si solo viene UNA fecha, NO aplica filtro de rango (cubre rama fInicio!=null && fFin!=null)")
 void buscar_soloUnaFecha_noAplicaFiltroFechas() throws Exception {
     Inmueble i1 = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);
     i1.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

     Inmueble i2 = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 50.0, false);
     i2.setDisponibilidad(disponibilidad(d("2025-12-01"), d("2025-12-05"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

     when(inmuebleDAO.findAll()).thenReturn(List.of(i1, i2));

     mockMvc.perform(post("/buscar")
                     .param("fechaInicio", "2025-12-12")
             )
             .andExpect(status().isOk())
             .andExpect(view().name("busqueda"))
             
             .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(2)));
 }

 @Test
 @DisplayName("POST /confirmarReserva -> precioPorNoche null => precioAplicado 0.0 y total 0 (cubre ternario precioAplicado)")
 void confirmarReserva_precioNull_precioAplicadoCero() throws Exception {
     Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 0.0, false);
     i.setPrecioPorNoche(null); 
     i.setPropietario(propietarioMinimo(20L, "Prop"));

    
     i.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.REEMBOLSABLE_50_PER));

     when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));
     when(reservaDAO.existsSolapamiento(eq(1L), any(Date.class), any(Date.class))).thenReturn(false);

     Inquilino inq = inquilinoMinimo(10L, "Ana");

     mockMvc.perform(post("/confirmarReserva")
                     .param("inmuebleId", "1")
                     .param("fechaInicio", "2025-12-12")
                     .param("fechaFin", "2025-12-15")
                     .sessionAttr("usuario", inq))
             .andExpect(status().isOk())
             .andExpect(view().name("reservaConfirmada"))
             .andExpect(model().attribute("total", 0.0));

     ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
     verify(reservaDAO).save(captor.capture());
     Reserva guardada = captor.getValue();

     assertThat(guardada.getPrecioPorNocheAplicado()).isEqualTo(0.0);
     assertThat(guardada.getPrecioTotal()).isEqualTo(0.0);
 }

 @Test
 @DisplayName("POST /confirmarReserva -> disponibilidad con politica null => politicaAplicada null (cubre rama polAplicada)")
 void confirmarReserva_politicaNull_politicaAplicadaNull() throws Exception {
     Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 10.0, false);
     i.setPropietario(propietarioMinimo(20L, "Prop"));

     Disponibilidad disp = new Disponibilidad();
     disp.setFechaInicio(d("2025-12-10"));
     disp.setFechaFin(d("2025-12-20"));
     disp.setDirecta(false);
     disp.setPoliticaCancelacion(null); 
     i.setDisponibilidad(disp);

     when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));
     when(reservaDAO.existsSolapamiento(eq(1L), any(Date.class), any(Date.class))).thenReturn(false);

     Inquilino inq = inquilinoMinimo(10L, "Ana");

     mockMvc.perform(post("/confirmarReserva")
                     .param("inmuebleId", "1")
                     .param("fechaInicio", "2025-12-12")
                     .param("fechaFin", "2025-12-13")
                     .sessionAttr("usuario", inq))
             .andExpect(status().isOk())
             .andExpect(view().name("reservaConfirmada"))
             .andExpect(model().attribute("esDirecta", false));

     ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
     verify(reservaDAO).save(captor.capture());
     Reserva guardada = captor.getValue();

     assertThat(guardada.getPoliticaCancelacionAplicada()).isNull();
     assertThat(guardada.getReservaDirectaAplicada()).isFalse();
 }

@Test
@DisplayName("POST /buscar -> fechaInicio/fechaFin vacías: NO parsea y NO rompe (cubre ramas !isEmpty=false)")
void buscar_fechasVacias_noParsea_noRompe() throws Exception {
  Inmueble i1 = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
  when(inmuebleDAO.findAll()).thenReturn(List.of(i1));

  mockMvc.perform(post("/buscar")
                  .param("fechaInicio", "")  
                  .param("fechaFin", ""))    
          .andExpect(status().isOk())
          .andExpect(view().name("busqueda"))
          .andExpect(model().attribute("busquedaRealizada", true))
          .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
}

@Test
@DisplayName("POST /buscar -> destino por CIUDAD (cubre rama ciudad.contains=true) y excluye cuando ciudad/dirección no valen")
void buscar_destino_porCiudad_y_excluye_sinMatch() throws Exception {
  Inmueble matchCiudad = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);

  Inmueble sinMatch = inmuebleBasico(2L, "Toledo", "Calle B", "Vivienda", 50.0, false);

  
  Inmueble dirNullCiudadNullPeroPasaFiltroInicial = inmuebleBasico(3L, "X", "Y", "Vivienda", 10.0, false);
  dirNullCiudadNullPeroPasaFiltroInicial.setCiudad(null);
  dirNullCiudadNullPeroPasaFiltroInicial.setDireccion(null); 

  when(inmuebleDAO.findAll()).thenReturn(List.of(matchCiudad, sinMatch, dirNullCiudadNullPeroPasaFiltroInicial));

  mockMvc.perform(post("/buscar")
                  .param("destino", "madrid"))
          .andExpect(status().isOk())
          .andExpect(view().name("busqueda"))
          .andExpect(model().attribute("busquedaRealizada", true))
          .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
}

@Test
@DisplayName("POST /buscar -> precioMax > 0 excluye inmuebles con precio > max (cubre rama <= false)")
void buscar_precioMax_excluyePrecioMayor() throws Exception {
  Inmueble barato = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);
  Inmueble caro = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 150.0, false);

  when(inmuebleDAO.findAll()).thenReturn(List.of(barato, caro));

  mockMvc.perform(post("/buscar")
                  .param("precioMax", "100"))
          .andExpect(status().isOk())
          .andExpect(view().name("busqueda"))
          .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
}

@Test
@DisplayName("POST /buscar -> filtro de fechas: excluye disp null/fechas null y fuera de rango (cubre ramas de lambda fechas)")
void buscar_fechas_filtra_nulos_y_fueraRango() throws Exception {
  Date fIni = d("2025-12-12");
  Date fFin = d("2025-12-15");

  
  Inmueble ok = inmuebleBasico(1L, "Madrid", "A", "Vivienda", 50.0, false);
  ok.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

 
  Inmueble dispNull = inmuebleBasico(2L, "Madrid", "B", "Vivienda", 50.0, false);
  dispNull.setDisponibilidad(null);

  
  Inmueble dispIniNull = inmuebleBasico(3L, "Madrid", "C", "Vivienda", 50.0, false);
  Disponibilidad d1 = new Disponibilidad();
  d1.setFechaInicio(null);
  d1.setFechaFin(d("2025-12-20"));
  d1.setDirecta(true);
  dispIniNull.setDisponibilidad(d1);


  Inmueble dispFinNull = inmuebleBasico(4L, "Madrid", "D", "Vivienda", 50.0, false);
  Disponibilidad d2 = new Disponibilidad();
  d2.setFechaInicio(d("2025-12-10"));
  d2.setFechaFin(null);
  d2.setDirecta(true);
  dispFinNull.setDisponibilidad(d2);


  Inmueble fueraBefore = inmuebleBasico(5L, "Madrid", "E", "Vivienda", 50.0, false);
  fueraBefore.setDisponibilidad(disponibilidad(d("2025-12-13"), d("2025-12-20"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

  
  Inmueble fueraAfter = inmuebleBasico(6L, "Madrid", "F", "Vivienda", 50.0, false);
  fueraAfter.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-14"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

  when(inmuebleDAO.findAll()).thenReturn(List.of(ok, dispNull, dispIniNull, dispFinNull, fueraBefore, fueraAfter));

  mockMvc.perform(post("/buscar")
                  .param("fechaInicio", "2025-12-12")
                  .param("fechaFin", "2025-12-15"))
          .andExpect(status().isOk())
          .andExpect(view().name("busqueda"))
          .andExpect(model().attribute("resultados", org.hamcrest.Matchers.hasSize(1)));
}

@Test
@DisplayName("POST /confirmarReserva -> disponibilidad existe pero fechaInicio null: cae en validación 2 (cubre rama fechaInicio null)")
void confirmarReserva_disponibilidad_fechaInicioNull_error() throws Exception {
  Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
  i.setPropietario(propietarioMinimo(20L, "Prop"));

  Disponibilidad disp = new Disponibilidad();
  disp.setFechaInicio(null);              
  disp.setFechaFin(d("2025-12-20"));
  disp.setDirecta(true);
  disp.setPoliticaCancelacion(PoliticaCancelacion.NO_REEMBOLSABLE);
  i.setDisponibilidad(disp);

  when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

  mockMvc.perform(post("/confirmarReserva")
                  .param("inmuebleId", "1")
                  .param("fechaInicio", "2025-12-12")
                  .param("fechaFin", "2025-12-13")
                  .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
          .andExpect(status().isOk())
          .andExpect(view().name("formularioReserva"))
          .andExpect(model().attributeExists("error"));

  verify(reservaDAO, never()).save(any());
}

@Test
@DisplayName("POST /confirmarReserva -> disponibilidad existe pero fechaFin null: cae en validación 2 (cubre rama fechaFin null)")
void confirmarReserva_disponibilidad_fechaFinNull_error() throws Exception {
  Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
  i.setPropietario(propietarioMinimo(20L, "Prop"));

  Disponibilidad disp = new Disponibilidad();
  disp.setFechaInicio(d("2025-12-10"));
  disp.setFechaFin(null);                 
  disp.setDirecta(true);
  disp.setPoliticaCancelacion(PoliticaCancelacion.NO_REEMBOLSABLE);
  i.setDisponibilidad(disp);

  when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

  mockMvc.perform(post("/confirmarReserva")
                  .param("inmuebleId", "1")
                  .param("fechaInicio", "2025-12-12")
                  .param("fechaFin", "2025-12-13")
                  .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
          .andExpect(status().isOk())
          .andExpect(view().name("formularioReserva"))
          .andExpect(model().attributeExists("error"));

  verify(reservaDAO, never()).save(any());
}

@Test
@DisplayName("POST /confirmarReserva -> fuera de rango por fechaFin AFTER dispFin (cubre rama reserva.getFechaFin().after(dispFin))")
void confirmarReserva_fueraDeRango_porFechaFinAfter_error() throws Exception {
  Inmueble i = inmuebleBasico(1L, "Madrid", "Calle A", "Vivienda", 50.0, false);
  i.setPropietario(propietarioMinimo(20L, "Prop"));
  i.setDisponibilidad(disponibilidad(d("2025-12-10"), d("2025-12-14"), true, PoliticaCancelacion.NO_REEMBOLSABLE));

  when(inmuebleDAO.findById(1L)).thenReturn(Optional.of(i));

  mockMvc.perform(post("/confirmarReserva")
                  .param("inmuebleId", "1")
                  .param("fechaInicio", "2025-12-12")
                  .param("fechaFin", "2025-12-15") 
                  .sessionAttr("usuario", inquilinoMinimo(10L, "Ana")))
          .andExpect(status().isOk())
          .andExpect(view().name("formularioReserva"))
          .andExpect(model().attributeExists("error"));

  verify(reservaDAO, never()).save(any());
}


    
}
