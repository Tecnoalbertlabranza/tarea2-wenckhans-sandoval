package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato JSON del endpoint de detalle; independiente del modelo del CSV. */
public record CompraDetalleDto(long id, String producto, BigDecimal monto, String unidad,
        LocalDate fecha, @JsonProperty("valor_unidad") BigDecimal valorUnidad,
        @JsonProperty("monto_pesos") long montoPesos) {
}
