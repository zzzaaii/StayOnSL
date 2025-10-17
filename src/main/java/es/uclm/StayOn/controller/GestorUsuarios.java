package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UsuarioDAO usuarioDAO;

    // 🔹 Mostrar formulario de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        return "registro";
    }

    // 🔹 Procesar registro (ya no usamos @ModelAttribute Usuario)
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
            model.addAttribute("error", "El email de usuario ya está registrado.");
            return "registro";
        }

        Usuario nuevoUsuario;

        // Crear tipo según el rol
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

        return "redirect:/registroExitoso";
    }

    // 🔹 Página de éxito
    @GetMapping("/registroExitoso")
    public String registroExitoso() {
        return "registroExitoso";
    }

    // 🔹 Mostrar login
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    // 🔹 Procesar login
    @PostMapping("/login")
    public String login(@RequestParam String login,
                        @RequestParam String pass,
                        HttpSession session,
                        Model model) {

        Usuario usuario = usuarioDAO.findByLogin(login);

        if (usuario == null) {
            model.addAttribute("error", "El usuario no existe.");
            return "login";
        }

        if (!usuario.getPass().equals(pass)) {
            model.addAttribute("error", "Contraseña incorrecta.");
            return "login";
        }

        session.setAttribute("usuario", usuario);

        if (usuario instanceof Propietario) {
            return "redirect:/inicioPropietario";
        } else {
            return "redirect:/inicioInquilino";
        }
    }

    // 🔹 Cerrar sesión
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/inicio";
    }
}

