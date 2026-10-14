package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
public record Compra(long id, String producto, BigDecimal monto, String unidad, LocalDate fecha) {
}
