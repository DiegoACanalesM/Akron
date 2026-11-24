package com.negocio.analitica.service;

import com.negocio.analitica.model.Cliente;
import com.negocio.analitica.repository.ClienteRepository;
import java.util.List;

public class ClienteService implements ClienteLookup {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        if (repository == null) throw new IllegalArgumentException("ClienteRepository cannot be null.");
        this.repository = repository;
    }

    public Cliente crear(Cliente cliente) {
        validar(cliente);
        normalizarDocumento(cliente);
        validarDocumentoUnico(cliente);
        return repository.save(cliente);
    }

    public Cliente actualizar(Cliente cliente) {
        validar(cliente);
        if (cliente.getId() == null || repository.findById(cliente.getId()).isEmpty()) {
            throw new IllegalArgumentException("Cliente no encontrado.");
        }
        normalizarDocumento(cliente);
        validarDocumentoUnico(cliente);
        return repository.save(cliente);
    }

    public Cliente buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
    }

    public List<Cliente> listar() { return repository.findAll(); }

    public void eliminar(Long id) {
        if (repository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Cliente no encontrado.");
        }
        Cliente customer = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        customer.setActive(false);
        repository.save(customer);
    }

    private void validar(Cliente cliente) {
        if (cliente == null) throw new IllegalArgumentException("Cliente obligatorio.");
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        // A customer document is optional in the PostgreSQL model.
        // If supplied, uniqueness is enforced by the repository/database.
    }

    private void validarDocumentoUnico(Cliente cliente) {
        if (cliente.getDocumento() == null) return;
        repository.findByDocumento(cliente.getDocumento()).ifPresent(existing -> {
            if (cliente.getId() == null || !cliente.getId().equals(existing.getId())) {
                throw new IllegalArgumentException("Ya existe un cliente con ese documento.");
            }
        });
    }

    private void normalizarDocumento(Cliente cliente) {
        if (cliente.getDocumento() != null) {
            String document = cliente.getDocumento().trim();
            cliente.setDocumento(document.isEmpty() ? null : document);
        }
    }
}
