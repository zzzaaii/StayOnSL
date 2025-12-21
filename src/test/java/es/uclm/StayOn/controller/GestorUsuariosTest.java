package es.uclm.StayOn.controller;

import es.uclm.StayOn.entity.Inmueble;
import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.entity.Reserva;
import es.uclm.StayOn.entity.Usuario;
import es.uclm.StayOn.persistence.UsuarioDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GestorUsuarios.class)
class GestorUsuariosTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioDAO usuarioDAO;

    @MockBean
    private GestorNotificaciones gestorNotificaciones;

  
    @Test
    @DisplayName("GET /registro -> devuelve vista registro")
    void mostrarFormularioRegistro_ok() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"));
    }

    
    @Test
    @DisplayName("POST /registro -> si login ya existe: vuelve a registro con error")
    void registrarUsuario_loginYaExiste_devuelveRegistroConError() throws Exception {
        when(usuarioDAO.findByLogin("a@a.com")).thenReturn(new Inquilino());

        mockMvc.perform(post("/registro")
                        .param("rol", "INQUILINO")
                        .param("login", "a@a.com")
                        .param("pass", "1234")
                        .param("nombre", "Ana")
                        .param("apellidos", "Lopez")
                        .param("direccion", "Calle X"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("error"));

        verify(usuarioDAO, never()).save(any());
        verify(gestorNotificaciones, never()).enviar(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /registro -> rol PROPIETARIO: crea Propietario, guarda y notifica, redirect /registroExitoso")
    void registrarUsuario_propietario_ok() throws Exception {
        when(usuarioDAO.findByLogin("p@p.com")).thenReturn(null);

        mockMvc.perform(post("/registro")
                        .param("rol", "PROPIETARIO")
                        .param("login", "p@p.com")
                        .param("pass", "1234")
                        .param("nombre", "Pepe")
                        .param("apellidos", "Perez")
                        .param("direccion", "Calle P"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registroExitoso"));

        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioDAO, times(1)).save(cap.capture());

        Usuario guardado = cap.getValue();
        assertThat(guardado).isInstanceOf(Propietario.class);
        assertThat(guardado.getLogin()).isEqualTo("p@p.com");
        assertThat(guardado.getPass()).isEqualTo("1234");
        assertThat(guardado.getNombre()).isEqualTo("Pepe");
        assertThat(guardado.getApellidos()).isEqualTo("Perez");
        assertThat(guardado.getDireccion()).isEqualTo("Calle P");

        verify(gestorNotificaciones, times(1)).enviar(
                same(guardado),
                eq("USUARIO_REGISTRO"),
                contains("Bienvenido a StayOn")
        );
    }

    @Test
    @DisplayName("POST /registro -> rol INQUILINO: crea Inquilino, guarda y notifica, redirect /registroExitoso")
    void registrarUsuario_inquilino_ok() throws Exception {
        when(usuarioDAO.findByLogin("i@i.com")).thenReturn(null);

        mockMvc.perform(post("/registro")
                        .param("rol", "INQUILINO")
                        .param("login", "i@i.com")
                        .param("pass", "abcd")
                        .param("nombre", "Irene")
                        .param("apellidos", "Iglesias")
                        .param("direccion", "Calle I"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registroExitoso"));

        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioDAO, times(1)).save(cap.capture());

        Usuario guardado = cap.getValue();
        assertThat(guardado).isInstanceOf(Inquilino.class);
        assertThat(guardado.getLogin()).isEqualTo("i@i.com");

        verify(gestorNotificaciones, times(1)).enviar(
                same(guardado),
                eq("USUARIO_REGISTRO"),
                contains("Irene")
        );
    }

    
    @Test
    @DisplayName("GET /registroExitoso -> devuelve vista registroExitoso")
    void registroExitoso_ok() throws Exception {
        mockMvc.perform(get("/registroExitoso"))
                .andExpect(status().isOk())
                .andExpect(view().name("registroExitoso"));
    }

 
    @Test
    @DisplayName("GET /login -> devuelve vista login")
    void mostrarLogin_ok() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

   
    @Test
    @DisplayName("POST /login -> usuario no existe: vuelve a login con error")
    void login_usuarioNoExiste_devuelveLoginConError() throws Exception {
        when(usuarioDAO.findByLogin("x@x.com")).thenReturn(null);

        mockMvc.perform(post("/login")
                        .param("login", "x@x.com")
                        .param("pass", "1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));

        verify(gestorNotificaciones, never()).enviar(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /login -> pass incorrecta: vuelve a login con error")
    void login_passIncorrecta_devuelveLoginConError() throws Exception {
        Inquilino u = new Inquilino();
        u.setId(1L);
        u.setLogin("a@a.com");
        u.setPass("bien");

        when(usuarioDAO.findByLogin("a@a.com")).thenReturn(u);

        mockMvc.perform(post("/login")
                        .param("login", "a@a.com")
                        .param("pass", "mal"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));

        verify(gestorNotificaciones, never()).enviar(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /login -> propietario correcto: guarda usuario en sesión, notifica y redirect /inicioPropietario")
    void login_propietario_ok() throws Exception {
        Propietario p = new Propietario();
        p.setId(2L);
        p.setLogin("p@p.com");
        p.setPass("1234");

        when(usuarioDAO.findByLogin("p@p.com")).thenReturn(p);

        mockMvc.perform(post("/login")
                        .param("login", "p@p.com")
                        .param("pass", "1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicioPropietario"))
                .andExpect(request().sessionAttribute("usuario", p));

        verify(gestorNotificaciones, times(1)).enviar(
                same(p),
                eq("LOGIN"),
                contains("iniciado sesión")
        );
    }

    @Test
    @DisplayName("POST /login -> inquilino correcto: guarda usuario en sesión, notifica y redirect /inicioInquilino")
    void login_inquilino_ok() throws Exception {
        Inquilino i = new Inquilino();
        i.setId(3L);
        i.setLogin("i@i.com");
        i.setPass("abcd");

        when(usuarioDAO.findByLogin("i@i.com")).thenReturn(i);

        mockMvc.perform(post("/login")
                        .param("login", "i@i.com")
                        .param("pass", "abcd"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicioInquilino"))
                .andExpect(request().sessionAttribute("usuario", i));

        verify(gestorNotificaciones, times(1)).enviar(
                same(i),
                eq("LOGIN"),
                contains("iniciado sesión")
        );
    }

 
    @Test
    @DisplayName("GET /logout -> si hay usuario en sesión: notifica LOGOUT e invalida y redirect /inicio")
    void logout_conUsuario_notificaYRedirect() throws Exception {
        Inquilino i = new Inquilino();
        i.setId(3L);
        i.setLogin("i@i.com");
        i.setPass("abcd");

        mockMvc.perform(get("/logout").sessionAttr("usuario", i))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verify(gestorNotificaciones, times(1)).enviar(
                same(i),
                eq("LOGOUT"),
                contains("Has cerrado sesión")
        );
    }

    @Test
    @DisplayName("GET /logout -> si NO hay usuario en sesión: no notifica y redirect /inicio")
    void logout_sinUsuario_noNotifica() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verify(gestorNotificaciones, never()).enviar(any(), anyString(), anyString());
    }
    @Test
    void getYSetInmuebles_ok() {
        Propietario propietario = new Propietario();

        Inmueble i1 = new Inmueble();
        Inmueble i2 = new Inmueble();

        List<Inmueble> inmuebles = List.of(i1, i2);

        propietario.setInmuebles(inmuebles);

        assertThat(propietario.getInmuebles())
                .isNotNull()
                .hasSize(2)
                .containsExactly(i1, i2);
    }
    
    @Test
    void getYSetReservas_ok() {
        Inquilino inquilino = new Inquilino();

        Reserva r1 = new Reserva();
        Reserva r2 = new Reserva();

        List<Reserva> reservas = List.of(r1, r2);

        inquilino.setReservas(reservas);

        assertThat(inquilino.getReservas())
                .isNotNull()
                .hasSize(2)
                .containsExactly(r1, r2);
    }
    @Test
    void getYSetAttribute_ok() {
        Usuario usuario = new Usuario() {
            
        };

        usuario.setAttribute(42);

        assertThat(usuario.getAttribute()).isEqualTo(42);
    }
    
    
    
}
