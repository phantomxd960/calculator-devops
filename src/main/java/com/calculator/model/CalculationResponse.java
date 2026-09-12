package com.calculator.model;

import java.util.Objects;

public class CalculationResponse {
    private String operation;
    private double a;
    private double b;
    private double result;

    public CalculationResponse() {
    }

    public CalculationResponse(String operation, double a, double b, double result) {
        this.operation = operation;
        this.a = a;
        this.b = b;
        this.result = result;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalculationResponse that = (CalculationResponse) o;
        return Double.compare(that.a, a) == 0 &&
               Double.compare(that.b, b) == 0 &&
               Double.compare(that.result, result) == 0 &&
               Objects.equals(operation, that.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, a, b, result);
    }

    @Override
    public String toString() {
        return "CalculationResponse{" +
                "operation='" + operation + '\'' +
                ", a=" + a +
                ", b=" + b +
                ", result=" + result +
                '}';
    }
}
