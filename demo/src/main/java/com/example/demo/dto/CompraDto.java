package com.example.demo.dto;

import java.math.BigDecimal;

public record CompraDto(
        long id,
        String producto,
        BigDecimal monto,
        String fecha,
        BigDecimal valor_unidad,
        long monto_pesos
) {}
