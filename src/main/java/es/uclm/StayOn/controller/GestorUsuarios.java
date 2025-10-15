package es.uclm.StayOn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import es.uclm.StayOn.entity.Inquilino;
import es.uclm.StayOn.entity.Propietario;
import es.uclm.StayOn.entity.Usuario;
import es.uclm.StayOn.persistence.UsuarioDAO;

@Controller
public class GestorUsuarios {

    @Autowired
    private UsuarioDAO usuarioDAO;

    // Muestra el formulario de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Propietario()); // Por defecto
        return "registro";
    }

    // Procesa registro
    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario, @RequestParam String rol, Model model) {
        Usuario nuevoUsuario;

        if ("PROPIETARIO".equals(rol)) {
            nuevoUsuario = new Propietario();
        } else {
            nuevoUsuario = new Inquilino();
        }

        nuevoUsuario.setLogin(usuario.getLogin());
        nuevoUsuario.setPass(usuario.getPass());
        nuevoUsuario.setNombre(usuario.getNombre());
        nuevoUsuario.setApellidos(usuario.getApellidos());
        nuevoUsuario.setDireccion(usuario.getDireccion());

        try {
            usuarioDAO.save(nuevoUsuario);
        } catch (Exception e) {
            model.addAttribute("error", "El email de usuario ya está registrado.");
            model.addAttribute("usuario", usuario);
            return "registro";
        }

        return "redirect:/registroExitoso";
    }

    @GetMapping("/registroExitoso")
    public String registroExitoso() {
        return "registroExitoso";
    }

    // Muestra formulario login
    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        return "login";
    }

    // Procesa login
    @PostMapping("/login")
    public String login(@RequestParam String login,
                        @RequestParam String pass,
                        Model model,
                        HttpSession session) {

        Usuario usuario = usuarioDAO.findByLogin(login);

        if (usuario == null) {
            model.addAttribute("error", "El usuario no existe.");
            return "login";
        }

        if (!usuario.getPass().equals(pass)) {
            model.addAttribute("error", "Contraseña incorrecta.");
            return "login";
        }

        // Guardamos el usuario en sesión
        session.setAttribute("usuario", usuario);

        // Redirigimos según tipo
        if (usuario instanceof Propietario) {
            return "redirect:/inicioPropietario";
        } else {
            return "redirect:/inicioInquilino";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Limpiamos sesión
        return "redirect:/inicio";
    }
}

