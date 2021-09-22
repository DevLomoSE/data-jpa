package com.devlomose.springboot.data.jpa.app.controllers;

import com.devlomose.springboot.data.jpa.app.models.dao.ClienteDAO;
import com.devlomose.springboot.data.jpa.app.models.entity.Cliente;
import com.devlomose.springboot.data.jpa.app.models.service.ClienteService;
import com.devlomose.springboot.data.jpa.app.util.paginator.PageRender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename){
        Path pathPhoto = Paths.get("uploads").resolve(filename).toAbsolutePath();
        logger.info("pathPhoto" + pathPhoto);

        Resource recurso = null;
        try {
            recurso = new UrlResource(pathPhoto.toUri());
            if(!recurso.exists() && !recurso.isReadable()){
                logger.error("Error el recurso "+pathPhoto.toString()+" no existe y/o no se puede leer");
                throw new RuntimeException("Error: no se puede cargar la imagen "+pathPhoto.toString());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        logger.info("recurso a cargar: "+recurso.getFilename());
        return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+recurso.getFilename()+"\"")
                            .body(recurso);
    }

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

    @GetMapping("/listado")
    public String list(@RequestParam(name="page", defaultValue="0") int page, Model model){
        Pageable pageRequest = PageRequest.of(page, 4);

        Page<Cliente> clientes =  clienteService.findAll(pageRequest);

        PageRender<Cliente> pageRender = new PageRender<>("/cliente/listado", clientes);

        model.addAttribute("titulo", "Listado Cliente");
        model.addAttribute("clientes", clientes);
        model.addAttribute("page", pageRender);
        return "clientes/listar";
    }

    @GetMapping("/form")
    public String createForm(Map<String, Object> model){
        Cliente cliente = new Cliente();

        model.put("titulo", "Crear Cliente");
        model.put("cliente", cliente);

        return "clientes/form";

    }

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

                Path rootPath = Paths.get("uploads").resolve(cliente.getFoto()).toAbsolutePath();
                File file = rootPath.toFile();
                if(file.exists() && file.canRead()){
                    file.delete();
                }
            }

            String uniqueFileName = UUID.randomUUID().toString() + foto.getOriginalFilename();

            Path rootPath = Paths.get("uploads").resolve( uniqueFileName );
            Path absolutPath = rootPath.toAbsolutePath();

            logger.info("rootPath " + rootPath);
            logger.info("absolutePath: " + absolutPath);

            try {
                Files.copy(foto.getInputStream(), absolutPath);

                flash.addFlashAttribute("info", " Has subido correctamente '" +
                                                    uniqueFileName +
                                                    "'");
                cliente.setFoto(uniqueFileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String flashMessage = (cliente.getId() != null)
                                ? "Cliente modificado exitosamente"
                                : "Cliente creado exitosamente";

        clienteService.save(cliente);
        sessionStatus.setComplete();
        flash.addFlashAttribute("success", flashMessage);
        return "redirect:/cliente/listado";
    }

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

    @GetMapping("/eliminar/{id}")
    public String deleteClient(@PathVariable(value="id") Long id, RedirectAttributes flash){
        if(id > 0){
            Cliente cliente = clienteService.findById(id);

            clienteService.delete(id);
            flash.addFlashAttribute("success", "Cliente eliminado exitosamente");

            Path rootPath = Paths.get("uploads").resolve(cliente.getFoto()).toAbsolutePath();
            File foto = rootPath.toFile();
            if(foto.exists() && foto.canRead()){
                if(foto.delete()){
                    flash.addFlashAttribute("info", "Foto " + cliente.getFoto() + " eliminada exitosamente");
                }
            }
        }
        return "redirect:/cliente/listado";
    }

}
