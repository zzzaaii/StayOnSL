package es.uclm.StayOn.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("usuario", new Propietario()); // Usamos uno por defecto
        return "registro";
    }
 // Procesa el envío del formulario de registro
    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario, @RequestParam String rol, Model model) {
        
        Usuario nuevoUsuario;

        if ("PROPIETARIO".equals(rol)) {
            nuevoUsuario = new Propietario();
        } else {
            nuevoUsuario = new Inquilino();
        }

        // Copiamos los datos del formulario al nuevo objeto del tipo correcto
        nuevoUsuario.setLogin(usuario.getLogin());
        nuevoUsuario.setPass(usuario.getPass()); // En un proyecto real, la contraseña debe ser encriptada!
        nuevoUsuario.setNombre(usuario.getNombre());
        nuevoUsuario.setApellidos(usuario.getApellidos());
        nuevoUsuario.setDireccion(usuario.getDireccion());

        try {
            usuarioDAO.save(nuevoUsuario);
        } catch (Exception e) {
            // Manejar error, por ejemplo, si el login ya existe
            model.addAttribute("error", "El email de usuario ya está registrado.");
            model.addAttribute("usuario", usuario);
            return "registro";
        }

        return "redirect:/registro-exitoso";
    }

    @GetMapping("/registro-exitoso")
    public String registroExitoso() {
        return "registro-exitoso";
    }
}
