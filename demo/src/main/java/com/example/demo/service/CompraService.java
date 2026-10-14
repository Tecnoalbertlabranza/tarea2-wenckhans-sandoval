package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.client.MindicadorClient;
import com.example.demo.dto.CompraDetalleDto;
import com.example.demo.dto.CompraDto;
import com.example.demo.dto.UnidadResponseDto;
import com.example.demo.model.Compra;
import com.example.demo.repository.CompraRepository;

@Service
public class CompraService {

    private final CompraRepository compras;
    private final MindicadorClient indicadores;

    public CompraService(CompraRepository compras, MindicadorClient indicadores) {
        this.compras = compras;
        this.indicadores = indicadores;
    }

    public UnidadResponseDto obtenerComprasPorUnidad(String unidadSolicitada) {
        String unidadNormalizada = unidadSolicitada.toUpperCase(Locale.ROOT);
        if (unidadNormalizada.equals("DOLAR")) {
            unidadNormalizada = "USD";
        }

        if (!unidadNormalizada.equals("UF") && !unidadNormalizada.equals("USD")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidad no soportada. Use 'uf' o 'dolar'.");
        }

        List<CompraDto> listaCompras = new ArrayList<>();
        long totalPesos = 0;
        for (Compra compra : compras.findAll()) {
            if (!unidadNormalizada.equals(compra.unidad())) {
                continue;
            }
            CompraDetalleDto detalle = convertir(compra);
            listaCompras.add(new CompraDto(detalle.id(), detalle.producto(), detalle.monto(),
                    detalle.fecha().toString(), detalle.valorUnidad(), detalle.montoPesos()));
            try {
                totalPesos = Math.addExact(totalPesos, detalle.montoPesos());
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El total excede el rango admitido.", ex);
            }
        }

        return new UnidadResponseDto(unidadNormalizada, listaCompras, totalPesos);
    }

    public CompraDetalleDto obtenerDetalle(long id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id debe ser un entero positivo.");
        }
        Compra compra = compras.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una compra con id " + id + "."));
        return convertir(compra);
    }

    private CompraDetalleDto convertir(Compra compra) {
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
