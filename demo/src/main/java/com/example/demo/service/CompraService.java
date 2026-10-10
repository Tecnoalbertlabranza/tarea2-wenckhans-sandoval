package com.example.demo.service;

import com.example.demo.client.IndicadorClient;
import com.example.demo.dto.CompraDetalleDto;
import com.example.demo.model.Compra;
import com.example.demo.repository.CompraRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompraService {
    private final CompraRepository compras;
    private final IndicadorClient indicadores;

    public CompraService(CompraRepository compras, IndicadorClient indicadores) {
        this.compras = compras;
        this.indicadores = indicadores;
    }

    public CompraDetalleDto obtenerDetalle(long id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id debe ser un entero positivo.");
        }
        Compra compra = compras.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una compra con id " + id + "."));
        BigDecimal valor = indicadores.obtenerValor(compra.unidad(), compra.fecha());
        long pesos;
        try {
            pesos = compra.monto().multiply(valor).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "El monto excede el rango admitido.", ex);
        }
        return new CompraDetalleDto(compra.id(), compra.producto(), compra.monto(), compra.unidad(),
                compra.fecha(), valor, pesos);
    }
}
