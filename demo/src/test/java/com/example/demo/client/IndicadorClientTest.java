package com.example.demo.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class IndicadorClientTest {
    private MockRestServiceServer server;
    private IndicadorClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://indicadores.test/api");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new IndicadorClient(builder.build());
    }

    @Test
    void queriesHistoricalUfAndIgnoresAdditionalProviderFields() {
        server.expect(requestTo("https://indicadores.test/api/uf/04-09-2026"))
                .andRespond(withSuccess("""
                    {"version":"1.7.0","autor":"mindicador.cl","codigo":"uf","nombre":"UF",
                     "unidad_medida":"Pesos","serie":[{"fecha":"2026-09-04T04:00:00.000Z","valor":40000.25}]}
                    """, MediaType.APPLICATION_JSON));
        assertEquals(new BigDecimal("40000.25"), client.obtenerValor("UF", LocalDate.of(2026, 9, 4)));
        server.verify();
    }

    @Test
    void mapsUsdToDolar() {
        server.expect(requestTo("https://indicadores.test/api/dolar/02-09-2026"))
                .andRespond(withSuccess("""
                    {"codigo":"dolar","serie":[{"fecha":"2026-09-02T04:00:00.000Z","valor":950.5}]}
                    """, MediaType.APPLICATION_JSON));
        assertEquals(new BigDecimal("950.5"), client.obtenerValor("USD", LocalDate.of(2026, 9, 2)));
        server.verify();
    }

    @Test
    void rejectsMissingHistoricalValue() {
        server.expect(requestTo("https://indicadores.test/api/uf/04-09-2026"))
                .andRespond(withSuccess("{\"codigo\":\"uf\",\"serie\":[]}", MediaType.APPLICATION_JSON));
        assertBadGateway();
    }

    @Test
    void doesNotUseValueFromAnotherDate() {
        server.expect(requestTo("https://indicadores.test/api/uf/04-09-2026"))
                .andRespond(withSuccess("""
                    {"codigo":"uf","serie":[{"fecha":"2026-09-03T04:00:00.000Z","valor":40000}]}
                    """, MediaType.APPLICATION_JSON));
        assertBadGateway();
    }

    @Test
    void handlesProviderFailure() {
        server.expect(requestTo("https://indicadores.test/api/uf/04-09-2026"))
                .andRespond(withServerError());
        assertBadGateway();
    }

    @Test
    void handlesMalformedJson() {
        server.expect(requestTo("https://indicadores.test/api/uf/04-09-2026"))
                .andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertBadGateway();
    }

    private void assertBadGateway() {
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> client.obtenerValor("UF", LocalDate.of(2026, 9, 4))).getStatusCode().value());
        server.verify();
    }
}
