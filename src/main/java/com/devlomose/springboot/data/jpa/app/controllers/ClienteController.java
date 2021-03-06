package com.devlomose.springboot.data.jpa.app.controllers;

import com.devlomose.springboot.data.jpa.app.models.entity.Cliente;
import com.devlomose.springboot.data.jpa.app.models.service.ClienteService;
import com.devlomose.springboot.data.jpa.app.models.service.UploadFileService;
import com.devlomose.springboot.data.jpa.app.util.paginator.PageRender;
import com.devlomose.springboot.data.jpa.app.view.xml.ClienteList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ClienteController at: src/main/java/com/devlomose/springboot/data/jpa/app/controllers
 * Created by @DevLomoSE at 14/9/21 10:46.
 */
@Controller
@RequestMapping("/cliente")
@SessionAttributes("cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private UploadFileService uploadFileService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MessageSource messageSource;

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename){

        Resource recurso = null;
        try {
            recurso = uploadFileService.load(filename);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+recurso.getFilename()+"\"")
                            .body(recurso);
    }

    @Secured({"ROLE_USER"})
    @GetMapping("/ver/{id}")
    public String getDetalle(@PathVariable(value="id") Long id, Map<String, Object> model, RedirectAttributes flash){
        Cliente cliente = clienteService.findById(id);
        if(cliente == null){
            flash.addFlashAttribute("error", "Cliente no existe en la BD");
            return "redirect:/cliente/listado";
        }
        model.put("cliente", cliente);
        model.put("titulo", "Detalle del cliente: " + cliente.getNombre());
        return "clientes/detalle";
    }

    @GetMapping({"/listado", "/"})
    public String list(@RequestParam(name="page", defaultValue="0") int page, Model model,
                       Authentication authentication,
                       HttpServletRequest httpServletRequest,
                       Locale locale){

        if(authentication != null){
            logger.info("Usuario autenticado: ".concat(authentication.getName()));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth != null){
            logger.info("Usuario autenticado: ".concat(auth.getName()).concat("    <- FORMA ESTATICA"));
        }

        if(hasRole("ROLE_ADMIN")){
            logger.info("Usuario ".concat(auth.getName()).concat(" acceso validado"));
        }else{
            logger.info("Usuario ".concat(auth.getName()).concat(" acceso no valido"));
        }

        SecurityContextHolderAwareRequestWrapper securityContext = new SecurityContextHolderAwareRequestWrapper(httpServletRequest, "ROLE_");
        if(securityContext.isUserInRole("ADMIN")){
            logger.info("Usuario ".concat(auth.getName()).concat(" acceso validado con securityContext"));
        }else{
            logger.info("Usuario ".concat(auth.getName()).concat(" acceso no valido con securityContext"));
        }

        if(httpServletRequest.isUserInRole("ROLE_ADMIN")){
            logger.info("Usuario ".concat(auth.getName()).concat(" acceso validado con httpServletRequest"));
        }else{
            logger.info("Usuario ".concat(auth.getName()).concat(" acceso no valido con httpServletRequest"));
        }

        Pageable pageRequest = PageRequest.of(page, 4);

        Page<Cliente> clientes =  clienteService.findAll(pageRequest);

        PageRender<Cliente> pageRender = new PageRender<>("/cliente/listado", clientes);

        model.addAttribute("titulo", messageSource.getMessage("text.cliente.listado.titulo",null, locale));
        model.addAttribute("clientes", clientes);
        model.addAttribute("page", pageRender);
        return "clientes/listar";
    }

    @GetMapping("/listado-rest")
    @ResponseBody
    public ClienteList listREST(){
        return new ClienteList(clienteService.findAll());
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/form")
    public String createForm(Map<String, Object> model){
        Cliente cliente = new Cliente();

        model.put("titulo", "Crear Cliente");
        model.put("cliente", cliente);

        return "clientes/form";

    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/form")
    public String saveCliente(@Valid Cliente cliente, BindingResult result, Model model,
                              @RequestParam("file") MultipartFile foto, RedirectAttributes flash,
                              SessionStatus sessionStatus){
        if(result.hasErrors()){
            model.addAttribute("titulo", "Crear Cliente");
            return "clientes/form";
        }

        if(!foto.isEmpty()){

            if(cliente.getId() != null
                    && cliente.getId() > 0
                    && cliente.getFoto() != null
                    && cliente.getFoto().length() > 0){

                uploadFileService.delete(cliente.getFoto());
            }

            String newFile = null;
            try {
                newFile = uploadFileService.copy(foto);
            } catch (IOException e) {
                e.printStackTrace();
            }

            flash.addFlashAttribute("info", " Has subido correctamente '" +
                    newFile +
                    "'");
            cliente.setFoto(newFile);
        }

        String flashMessage = (cliente.getId() != null)
                                ? "Cliente modificado exitosamente"
                                : "Cliente creado exitosamente";

        clienteService.save(cliente);
        sessionStatus.setComplete();
        flash.addFlashAttribute("success", flashMessage);
        return "redirect:/cliente/listado";
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/form/{id}")
    public String editClient(@PathVariable(value="id") Long id, Map<String, Object> model,
                            RedirectAttributes flash){
        Cliente cliente = null;
        if(id > 0){
            cliente = clienteService.findById(id);
            if(cliente == null){
                flash.addFlashAttribute("error", "El cliente no se encuentra en la BD");
                return "redirect:clientes/listado";
            }
        }else{
            flash.addFlashAttribute("error", "Error al buscar el cliente");
            return "redirect:clientes/listado";
        }

        model.put("cliente", cliente);
        model.put("titulo", "Editar cliente");
        return "clientes/form";
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/eliminar/{id}")
    public String deleteClient(@PathVariable(value="id") Long id, RedirectAttributes flash){
        if(id > 0){
            Cliente cliente = clienteService.findById(id);

            clienteService.delete(id);
            flash.addFlashAttribute("success", "Cliente eliminado exitosamente");

            if(uploadFileService.delete(cliente.getFoto())){
                flash.addFlashAttribute("info", "Foto " + cliente.getFoto() + " eliminada exitosamente");
            }
        }
        return "redirect:/cliente/listado";
    }


    private boolean hasRole(String role){
        SecurityContext securityContext = SecurityContextHolder.getContext();

        if(securityContext == null){
            return false;
        }

        Authentication auth = securityContext.getAuthentication();

        if(auth == null){
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

        /*
        for(GrantedAuthority authority: authorities){
            if(role.equals(authority.getAuthority())){
                return true;
            }
        }
        return false;
        */

        return authorities.contains(new SimpleGrantedAuthority(role));
    }
}
