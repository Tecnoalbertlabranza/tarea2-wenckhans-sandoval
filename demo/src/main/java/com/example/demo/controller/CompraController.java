package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UnidadResponseDto;
import com.example.demo.service.CompraService;

@RestController
@RequestMapping("/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping("/{unidad}")
    public ResponseEntity<UnidadResponseDto> getComprasPorUnidad(@PathVariable String unidad) {
        UnidadResponseDto response = compraService.obtenerComprasPorUnidad(unidad);
        return ResponseEntity.ok(response);
    }
}