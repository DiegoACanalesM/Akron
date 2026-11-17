package com.negocio.analitica.repository;

import com.negocio.analitica.model.MetodoPago;
import java.util.List;
import java.util.Optional;

/** Persistence contract for the payment_methods catalog. */
public interface MetodoPagoRepository {
    Optional<MetodoPago> findById(Long id);
    List<MetodoPago> findAllActive();
}
