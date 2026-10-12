package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

/** Campos necesarios de la respuesta del proveedor externo. */
public record IndicadorResponseDto(String codigo, List<ValorIndicadorDto> serie) {
    public record ValorIndicadorDto(String fecha, BigDecimal valor) { }
}
