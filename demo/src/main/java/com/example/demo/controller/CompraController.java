package com.example.demo.controller;

import com.example.demo.dto.CompraDetalleDto;
import com.example.demo.service.CompraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compras/detalle")
public class CompraController {
    private final CompraService compras;

    public CompraController(CompraService compras) {
        this.compras = compras;
    }

    @GetMapping("/{id}")
    public CompraDetalleDto detalle(@PathVariable long id) {
        return compras.obtenerDetalle(id);
    }
}
