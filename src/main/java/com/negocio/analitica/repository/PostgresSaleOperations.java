package com.negocio.analitica.repository;

import com.negocio.analitica.model.Venta;

/** PostgreSQL sale operations that require transactional stock coordination. */
public interface PostgresSaleOperations extends VentaRepository {
    Venta cancelCompletedSale(Long saleId, Long operatorId, String note);
}
