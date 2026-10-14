package com.example.demo.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class CompraRepositoryTest {
    @TempDir
    Path temp;
    private static final String HEADER = "id,producto,monto,unidad,fecha\n";

    @Test
    void resolvesCsvFromRootAndDemo() throws Exception {
        Path csv = temp.resolve("compras.csv");
        Path demo = Files.createDirectory(temp.resolve("demo"));
        Files.writeString(csv, HEADER + "3,Escritorio,8,UF,2026-09-04\n");
        for (Path directory : java.util.List.of(temp, demo)) {
            CompraRepository repository = new CompraRepository("compras.csv", directory);
            assertEquals(csv, repository.getCsvPath());
            assertEquals("Escritorio", repository.findById(3).orElseThrow().producto());
        }
    }

    @Test
    void preservesExplicitCsvPathEvenWhenMissing() throws Exception {
        Path demo = Files.createDirectory(temp.resolve("demo"));
        Files.writeString(temp.resolve("compras.csv"), HEADER);
        Path configured = demo.resolve("external.csv");
        CompraRepository repository = new CompraRepository(configured.toString(), demo);
        assertEquals(configured, repository.getCsvPath());
        assertThrows(ResponseStatusException.class, () -> repository.findById(3));
    }

    @Test
    void findsByIdRegardlessOfPositionAndReloadsExternalFile() throws Exception {
        Path csv = temp.resolve("compras.csv");
        Files.writeString(csv, HEADER + "8,SSD,120.5,USD,2026-09-22\n3,Escritorio,8,UF,2026-09-04\n");
        CompraRepository repository = new CompraRepository(csv.toString());
        assertEquals("Escritorio", repository.findById(3).orElseThrow().producto());
        assertTrue(repository.findById(99).isEmpty());
        Files.writeString(csv, HEADER + "3,Mesa,9,UF,2026-09-04\n");
        assertEquals("Mesa", repository.findById(3).orElseThrow().producto());
    }

    @Test
    void rejectsInvalidFilesAndMissingFiles() throws Exception {
        Path csv = temp.resolve("compras.csv");
        CompraRepository repository = new CompraRepository(csv.toString());
        assertEquals(500, assertThrows(ResponseStatusException.class,
                () -> repository.findById(1)).getStatusCode().value());
        for (String content : java.util.List.of("incorrect header\n",
                HEADER + "1,Mesa,no-es-numero,UF,2026-09-04\n",
                HEADER + "1,Mesa,8,EUR,2026-09-04\n",
                HEADER + "1,Mesa,8,UF,fecha-invalida\n",
                HEADER + "1,Mesa,8,UF,2026-09-04\n1,Silla,2,UF,2026-09-04\n")) {
            Files.writeString(csv, content);
            assertEquals(500, assertThrows(ResponseStatusException.class,
                    () -> repository.findById(1)).getStatusCode().value());
        }
    }
}
