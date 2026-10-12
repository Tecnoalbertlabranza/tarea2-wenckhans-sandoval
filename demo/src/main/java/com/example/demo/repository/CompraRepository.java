package com.example.demo.repository;

import com.example.demo.model.Compra;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class CompraRepository {
    private final Path csvPath;

    public CompraRepository(@Value("${compras.csv-path}") String csvPath) {
        this.csvPath = Path.of(csvPath);
    }

    public Optional<Compra> findById(long id) {
        try {
            var lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !lines.getFirst().replace("\uFEFF", "").trim()
                    .equals("id,producto,monto,unidad,fecha")) {
                throw new IllegalArgumentException("Encabezado CSV incorrecto");
            }
            var ids = new HashSet<Long>();
            Compra encontrada = null;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) {
                    continue;
                }
                String[] fields = lines.get(i).split(",", -1);
                if (fields.length != 5) {
                    throw new IllegalArgumentException("Cantidad de columnas incorrecta");
                }
                long compraId = Long.parseLong(fields[0].trim());
                String producto = fields[1].trim();
                BigDecimal monto = new BigDecimal(fields[2].trim());
                String unidad = fields[3].trim();
                LocalDate fecha = LocalDate.parse(fields[4].trim());
                if (compraId <= 0 || !ids.add(compraId) || producto.isBlank() || monto.signum() < 0
                        || !(unidad.equals("UF") || unidad.equals("USD"))) {
                    throw new IllegalArgumentException("Datos de compra incorrectos");
                }
                if (compraId == id) {
                    encontrada = new Compra(compraId, producto, monto, unidad, fecha);
                }
            }
            return Optional.ofNullable(encontrada);
        } catch (IOException | IllegalArgumentException | DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo leer el archivo de compras. Revise CSV_PATH y el formato del CSV.", ex);
        }
    }
}
