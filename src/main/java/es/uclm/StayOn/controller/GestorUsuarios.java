package es.uclm.StayOn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.entity.Usuario;
import es.uclm.StayOn.persistence.UsuarioDAO;

import jakarta.servlet.http.HttpSession;

@Controller
public class GestorUsuarios {

    private static final String ATTR_ERROR = "error";

    private static final String VIEW_LOGIN = "login";
    private static final String VIEW_REGISTRO = "registro";

    private final UsuarioDAO usuarioDAO;
    private final GestorNotificaciones gestorNotificaciones;

    public GestorUsuarios(UsuarioDAO usuarioDAO, GestorNotificaciones gestorNotificaciones) {
        this.usuarioDAO = usuarioDAO;
        this.gestorNotificaciones = gestorNotificaciones;
    }

    // Mostrar formulario de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        return VIEW_REGISTRO;
    }

    // Procesar registro
    @PostMapping("/registro")
    public String registrarUsuario(@RequestParam String rol,
                                   @RequestParam String login,
                                   @RequestParam String pass,
                                   @RequestParam String nombre,
                                   @RequestParam String apellidos,
                                   @RequestParam String direccion,
                                   Model model) {

        // Verificar si ya existe el usuario
        if (usuarioDAO.findByLogin(login) != null) {
            model.addAttribute(ATTR_ERROR, "El email de usuario ya está registrado.");
            return VIEW_REGISTRO;
        }

        Usuario nuevoUsuario;
        if ("PROPIETARIO".equalsIgnoreCase(rol)) {
            nuevoUsuario = new Propietario();
        } else {
            nuevoUsuario = new Inquilino();
        }

        nuevoUsuario.setLogin(login);
        nuevoUsuario.setPass(pass);
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setApellidos(apellidos);
        nuevoUsuario.setDireccion(direccion);

        usuarioDAO.save(nuevoUsuario);

        // Notificación de bienvenida
        gestorNotificaciones.enviar(
                nuevoUsuario,
                "USUARIO_REGISTRO",
                "🎉 Bienvenido a StayOn, " + nombre + ". Tu cuenta ha sido creada con éxito."
        );

        return "redirect:/registroExitoso";
    }

    @GetMapping("/registroExitoso")
    public String registroExitoso() {
        return "registroExitoso";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return VIEW_LOGIN;
    }

    // Procesar login
    @PostMapping("/login")
    public String login(@RequestParam String login,
                        @RequestParam String pass,
                        HttpSession session,
                        Model model) {

        Usuario usuario = usuarioDAO.findByLogin(login);

        if (usuario == null) {
            model.addAttribute(ATTR_ERROR, "El usuario no existe.");
            return VIEW_LOGIN;
        }

        if (!usuario.getPass().equals(pass)) {
            model.addAttribute(ATTR_ERROR, "Contraseña incorrecta.");
            return VIEW_LOGIN;
        }

        session.setAttribute("usuario", usuario);

        // Notificación de inicio de sesión
        gestorNotificaciones.enviar(
                usuario,
                "LOGIN",
                "👋 Has iniciado sesión correctamente en StayOn."
        );

        if (usuario instanceof Propietario) {
            return "redirect:/inicioPropietario";
        } else {
            return "redirect:/inicioInquilino";
        }
    }

    // Cerrar sesión
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            gestorNotificaciones.enviar(
                    usuario,
                    "LOGOUT",
                    "👋 Has cerrado sesión en StayOn. ¡Hasta pronto!"
            );
        }
        session.invalidate();
        return "redirect:/inicio";
    }
}
