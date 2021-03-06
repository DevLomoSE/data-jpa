package com.devlomose.springboot.data.jpa.app.models.service;

import com.devlomose.springboot.data.jpa.app.controllers.FacturaController;
import com.devlomose.springboot.data.jpa.app.models.dao.ClienteDAO;
import com.devlomose.springboot.data.jpa.app.models.dao.FacturaDAO;
import com.devlomose.springboot.data.jpa.app.models.dao.ProductoDAO;
import com.devlomose.springboot.data.jpa.app.models.entity.Cliente;
import com.devlomose.springboot.data.jpa.app.models.entity.Factura;
import com.devlomose.springboot.data.jpa.app.models.entity.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ClienteServiceImplement at: src/main/java/com/devlomose/springboot/data/jpa/app/models/service
 * Created by @DevLomoSE at 21/9/21 10:57.
 */
@Service
public class ClienteServiceImplement implements ClienteService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClienteDAO clienteDAO;

    @Autowired
    private ProductoDAO productoDAO;

    @Autowired
    private FacturaDAO facturaDAO;

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> findAll() {
        return (List<Cliente>) clienteDAO.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cliente> findAll(Pageable pageable) {
        return clienteDAO.findAll(pageable);
    }

    @Override
    @Transactional
    public void save(Cliente cliente) {
        clienteDAO.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente findById(Long id) {
        return clienteDAO.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        clienteDAO.deleteById(id);
    }

    @Override
    public List<Producto> findByName(String nombre) {
        return productoDAO.findByName(nombre);
    }

    @Override
    @Transactional
    public void saveFactura(Factura factura) {
        logger.info(factura.toString());
        facturaDAO.save(factura);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto findProductoById(Long id) {
        return productoDAO.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Factura findFacturaById(Long id) {
        return facturaDAO.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteFactura(Long id) {
        facturaDAO.deleteById(id);
    }
}
