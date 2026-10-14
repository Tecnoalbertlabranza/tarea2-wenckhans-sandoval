package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;
public record IndicadorResponseDto(String codigo, List<ValorIndicadorDto> serie) {
    public record ValorIndicadorDto(String fecha, BigDecimal valor) { }
}
