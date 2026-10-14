package com.example.demo.dto;

import java.util.List;

public record UnidadResponseDto(
       String unidad,
        List<CompraDto> compras,
        long total_pesos
) {}
