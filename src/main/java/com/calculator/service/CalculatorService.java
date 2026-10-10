package com.calculator.service;

import com.calculator.exception.DivisionByZeroException;
import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    public double add(double a, double b) {
        requireFinite(a, b);
        return checkResult(a + b);
    }

    public double subtract(double a, double b) {
        requireFinite(a, b);
        return checkResult(a - b);
    }

    public double multiply(double a, double b) {
        requireFinite(a, b);
        return checkResult(a * b);
    }

    public double divide(double a, double b) {
        requireFinite(a, b);
        if (b == 0.0) {
            throw new DivisionByZeroException("Division by zero is not allowed");
        }
        return checkResult(a / b);
    }

    // "NaN" and "Infinity" parse as valid doubles, so reject them explicitly
    private void requireFinite(double a, double b) {
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new IllegalArgumentException("Parameters 'a' and 'b' must be finite numbers");
        }
    }

    private double checkResult(double result) {
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Result is out of the representable numeric range");
        }
        return result;
    }
}
