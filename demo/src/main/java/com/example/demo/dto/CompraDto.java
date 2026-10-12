package com.example.demo.dto;

public record CompraDto(
        int id, 
        String producto, 
        double monto, 
        String fecha, 
        double valor_unidad, 
        int monto_pesos
) {}