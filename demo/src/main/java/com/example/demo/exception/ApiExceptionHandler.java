package com.example.demo.exception;

import com.example.demo.dto.ErrorResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleStatus(ResponseStatusException ex) {
        String mensaje = ex.getReason() == null ? "No se pudo procesar la solicitud." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponseDto(mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidId(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto("El id debe ser un entero positivo."));
    }
}
