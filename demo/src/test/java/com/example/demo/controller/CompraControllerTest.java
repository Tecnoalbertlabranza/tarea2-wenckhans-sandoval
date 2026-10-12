package com.example.demo.controller;

import com.example.demo.client.MindicadorClient;
import com.example.demo.exception.ApiExceptionHandler;
import com.example.demo.repository.CompraRepository;
import com.example.demo.service.CompraService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CompraControllerTest {
        @TempDir
        Path temp;
        private MockMvc mvc;
        private MindicadorClient indicadores;

        @BeforeEach
        void setup() throws Exception {
                Path csv = temp.resolve("compras.csv");
                Files.writeString(csv, "id,producto,monto,unidad,fecha\n"
                                + "3,Escritorio,8,UF,2026-09-04\n2,Monitor,180,USD,2026-09-02\n");
                indicadores = mock(MindicadorClient.class);
                CompraService service = new CompraService(new CompraRepository(csv.toString()), indicadores);
                mvc = MockMvcBuilders.standaloneSetup(new CompraController(service))
                                .setControllerAdvice(new ApiExceptionHandler()).build();
        }

        @Test
        void returnsExactUfContractAndRoundsHalfUp() throws Exception {
                when(indicadores.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                                .thenReturn(new BigDecimal("40000.0625"));
                mvc.perform(get("/compras/detalle/3")).andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(7))
                                .andExpect(jsonPath("$.id").value(3))
                                .andExpect(jsonPath("$.producto").value("Escritorio"))
                                .andExpect(jsonPath("$.monto").value(8))
                                .andExpect(jsonPath("$.unidad").value("UF"))
                                .andExpect(jsonPath("$.fecha").value("2026-09-04"))
                                .andExpect(jsonPath("$.valor_unidad").value(40000.0625))
                                .andExpect(jsonPath("$.monto_pesos").value(320001));
                verify(indicadores).obtenerValor("UF", LocalDate.of(2026, 9, 4));
        }

        @Test
        void convertsUsd() throws Exception {
                when(indicadores.obtenerValor("USD", LocalDate.of(2026, 9, 2)))
                                .thenReturn(new BigDecimal("950.12"));
                mvc.perform(get("/compras/detalle/2")).andExpect(status().isOk())
                                .andExpect(jsonPath("$.unidad").value("USD"))
                                .andExpect(jsonPath("$.monto_pesos").value(171022));
        }

        @Test
        void returns404WithoutConsultingProviderForUnknownPurchase() throws Exception {
                mvc.perform(get("/compras/detalle/999")).andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("No existe una compra con id 999."));
                verifyNoInteractions(indicadores);
        }

        @Test
        void rejectsInvalidIdentifiers() throws Exception {
                for (String id : new String[] { "abc", "0", "-1", "99999999999999999999999" }) {
                        mvc.perform(get("/compras/detalle/" + id)).andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").exists());
                }
                verifyNoInteractions(indicadores);
        }

        @Test
        void returns502OnProviderFailure() throws Exception {
                when(indicadores.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                                "Indicador no disponible."));
                mvc.perform(get("/compras/detalle/3")).andExpect(status().isBadGateway())
                                .andExpect(jsonPath("$.error").value("Indicador no disponible."));
        }

        @Test
        void reportsOverflowInsteadOfReturningIncorrectPesos() throws Exception {
                when(indicadores.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                                .thenReturn(new BigDecimal("999999999999999999999"));
                mvc.perform(get("/compras/detalle/3")).andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").value("El monto excede el rango admitido."));
        }
}
