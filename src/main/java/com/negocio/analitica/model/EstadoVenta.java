package com.negocio.analitica.model;

/** Values must remain synchronized with the sales.status database constraint. */
public enum EstadoVenta {
    DRAFT,
    COMPLETED,
    CANCELLED
}
