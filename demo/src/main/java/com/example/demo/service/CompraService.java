package com.example.demo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final String csvPath = System.getenv("CSV_PATH") != null ? System.getenv("CSV_PATH") : "compras.csv";

    public CompraService(CompraRepository compras, MindicadorClient indicadores) {
        this.compras = compras;
        this.indicadores = indicadores;
    }

    public UnidadResponseDto obtenerComprasPorUnidad(String unidadSolicitada) {
        String unidadNormalizada = unidadSolicitada.toUpperCase();
        if (unidadNormalizada.equals("DOLAR")) {
            unidadNormalizada = "USD";
        }

        if (!unidadNormalizada.equals("UF") && !unidadNormalizada.equals("USD")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidad no soportada. Use 'uf' o 'dolar'.");
        }

        List<CompraDto> listaCompras = new ArrayList<>();
        int totalPesos = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                String[] columnas = linea.split(",");
                if (columnas.length == 5 && columnas[3].trim().equalsIgnoreCase(unidadNormalizada)) {
                    
                    int id = Integer.parseInt(columnas[0].trim());
                    String producto = columnas[1].trim();
                    double monto = Double.parseDouble(columnas[2].trim());
                    String fechaCsv = columnas[4].trim();
                    LocalDate fecha = LocalDate.parse(fechaCsv);

                    BigDecimal valorBd = indicadores.obtenerValor(unidadNormalizada, fecha);
                    double valorUnidad = valorBd.doubleValue();
                    
                    int montoPesos = (int) Math.round(monto * valorUnidad);

                    listaCompras.add(new CompraDto(id, producto, monto, fechaCsv, valorUnidad, montoPesos));
                    totalPesos += montoPesos;
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo CSV: " + e.getMessage());
        }

        return new UnidadResponseDto(unidadNormalizada, listaCompras, totalPesos);
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