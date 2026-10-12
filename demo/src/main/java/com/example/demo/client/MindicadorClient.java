package com.example.demo.client;

import com.example.demo.dto.IndicadorResponseDto;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MindicadorClient {
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final ZoneId CHILE = ZoneId.of("America/Santiago");
    private final RestClient client;

    @Autowired
    public MindicadorClient(@Value("${indicadores.base-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    MindicadorClient(RestClient client) {
        this.client = client;
    }

    public BigDecimal obtenerValor(String unidad, LocalDate fecha) {
        String indicador = switch (unidad) {
            case "UF" -> "uf";
            case "USD" -> "dolar";
            default -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "La compra tiene una unidad no admitida.");
        };
        try {
            IndicadorResponseDto response = client.get()
                    .uri("/{indicador}/{fecha}", indicador, fecha.format(API_DATE))
                    .retrieve().body(IndicadorResponseDto.class);
            if (response == null || !indicador.equals(response.codigo()) || response.serie() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta de indicador inválida.");
            }
            for (var item : response.serie()) {
                if (item == null || item.fecha() == null || item.valor() == null || item.valor().signum() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta de indicador inválida.");
                }
                LocalDate fechaIndicador = OffsetDateTime.parse(item.fecha())
                        .atZoneSameInstant(CHILE).toLocalDate();
                if (fecha.equals(fechaIndicador)) {
                    return item.valor();
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No hay valor de " + unidad + " para la fecha " + fecha + ".");
        } catch (RestClientException | DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo obtener el indicador para la fecha de compra.", ex);
        }
    }
}
