package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Datos de una compra con el formato del CSV
public record Compra(long id, String producto, BigDecimal monto, String unidad, LocalDate fecha) {
}
