package com.calculator.controller;

import com.calculator.model.CalculationResponse;
import com.calculator.service.CalculatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @GetMapping("/add")
    public ResponseEntity<CalculationResponse> add(
            @RequestParam("a") double a,
            @RequestParam("b") double b) {
        double result = calculatorService.add(a, b);
        return ResponseEntity.ok(new CalculationResponse("add", a, b, result));
    }

    @GetMapping("/subtract")
    public ResponseEntity<CalculationResponse> subtract(
            @RequestParam("a") double a,
            @RequestParam("b") double b) {
        double result = calculatorService.subtract(a, b);
        return ResponseEntity.ok(new CalculationResponse("subtract", a, b, result));
    }

    @GetMapping("/multiply")
    public ResponseEntity<CalculationResponse> multiply(
            @RequestParam("a") double a,
            @RequestParam("b") double b) {
        double result = calculatorService.multiply(a, b);
        return ResponseEntity.ok(new CalculationResponse("multiply", a, b, result));
    }

    @GetMapping("/divide")
    public ResponseEntity<CalculationResponse> divide(
            @RequestParam("a") double a,
            @RequestParam("b") double b) {
        double result = calculatorService.divide(a, b);
        return ResponseEntity.ok(new CalculationResponse("divide", a, b, result));
    }
}
