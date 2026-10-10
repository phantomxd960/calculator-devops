package com.calculator.service;

import com.calculator.exception.DivisionByZeroException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CalculatorService Unit Tests")
class CalculatorServiceTest {

    private CalculatorService calculatorService;

    @BeforeEach
    void setUp() {
        calculatorService = new CalculatorService();
    }

    @Test
    @DisplayName("Should add two positive numbers correctly")
    void testAddPositiveNumbers() {
        double result = calculatorService.add(10.0, 20.0);
        assertEquals(30.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should add negative numbers correctly")
    void testAddNegativeNumbers() {
        double result = calculatorService.add(-5.0, -15.0);
        assertEquals(-20.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should add decimal numbers correctly")
    void testAddDecimals() {
        double result = calculatorService.add(2.5, 3.75);
        assertEquals(6.25, result, 0.0001);
    }

    @Test
    @DisplayName("Should subtract two numbers correctly")
    void testSubtract() {
        double result = calculatorService.subtract(10.0, 4.0);
        assertEquals(6.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should subtract resulting in negative value")
    void testSubtractNegativeResult() {
        double result = calculatorService.subtract(4.0, 10.0);
        assertEquals(-6.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should multiply two positive numbers correctly")
    void testMultiply() {
        double result = calculatorService.multiply(6.0, 7.0);
        assertEquals(42.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should multiply with zero resulting in zero")
    void testMultiplyByZero() {
        double result = calculatorService.multiply(100.0, 0.0);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should multiply negative numbers correctly")
    void testMultiplyNegativeNumbers() {
        double result = calculatorService.multiply(-4.0, 5.0);
        assertEquals(-20.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should divide two numbers correctly")
    void testDivide() {
        double result = calculatorService.divide(20.0, 4.0);
        assertEquals(5.0, result, 0.0001);
    }

    @Test
    @DisplayName("Should divide with decimal result")
    void testDivideDecimalResult() {
        double result = calculatorService.divide(1.0, 4.0);
        assertEquals(0.25, result, 0.0001);
    }

    @Test
    @DisplayName("Should throw DivisionByZeroException when denominator is zero")
    void testDivideByZeroThrowsException() {
        DivisionByZeroException exception = assertThrows(
                DivisionByZeroException.class,
                () -> calculatorService.divide(10.0, 0.0)
        );
        assertEquals("Division by zero is not allowed", exception.getMessage());
    }

    @ParameterizedTest(name = "add({0}, {1}) = {2}")
    @CsvSource({
            "1.0, 2.0, 3.0",
            "0.0, 0.0, 0.0",
            "-1.5, 1.5, 0.0",
            "100.25, 200.75, 301.0"
    })
    @DisplayName("Parameterized tests for addition")
    void parameterizedAddTest(double a, double b, double expected) {
        assertEquals(expected, calculatorService.add(a, b), 0.0001);
    }

    @Test
    @DisplayName("Should reject NaN and Infinity inputs")
    void testNonFiniteInputsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculatorService.add(Double.NaN, 1.0));
        assertThrows(IllegalArgumentException.class, () -> calculatorService.multiply(Double.POSITIVE_INFINITY, 2.0));
        assertThrows(IllegalArgumentException.class, () -> calculatorService.divide(1.0, Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Should reject results that overflow the double range")
    void testOverflowRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculatorService.multiply(Double.MAX_VALUE, 10.0));
    }
}
