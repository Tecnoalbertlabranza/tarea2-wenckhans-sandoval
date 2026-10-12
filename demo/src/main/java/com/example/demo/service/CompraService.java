package com.example.demo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.client.MindicadorClient;
import com.example.demo.dto.CompraDto;
import com.example.demo.dto.UnidadResponseDto;

@Service
public class CompraService {

    private final MindicadorClient mindicadorClient;
    private final String csvPath = System.getenv("CSV_PATH") != null ? System.getenv("CSV_PATH") : "compras.csv";

    public CompraService(MindicadorClient mindicadorClient) {
        this.mindicadorClient = mindicadorClient;
    }

    public UnidadResponseDto obtenerComprasPorUnidad(String unidadSolicitada) {
        String unidadNormalizada = unidadSolicitada.toUpperCase();
        if (unidadNormalizada.equals("DOLAR")) {
            unidadNormalizada = "USD";
        }

        if (!unidadNormalizada.equals("UF") && !unidadNormalizada.equals("USD")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidad no soportada. Use 'uf' o 'dolar'.");
        }

        List<CompraDto> compras = new ArrayList<>();
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

                    BigDecimal valorBd = mindicadorClient.obtenerValor(unidadNormalizada, fecha);
                    double valorUnidad = valorBd.doubleValue();
                    
                
                    int montoPesos = (int) Math.round(monto * valorUnidad);

                   
                    compras.add(new CompraDto(id, producto, monto, fechaCsv, valorUnidad, montoPesos));
                    totalPesos += montoPesos;
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo CSV: " + e.getMessage());
        }

        return new UnidadResponseDto(unidadNormalizada, compras, totalPesos);
    }
}