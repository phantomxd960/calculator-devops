package com.calculator.controller;

import com.calculator.service.CalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculatorController.class)
@Import(CalculatorService.class)
@DisplayName("CalculatorController MockMvc Integration Tests")
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/add?a=10&b=20 should return 30.0")
    void testAddEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "10")
                        .param("b", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.operation", is("add")))
                .andExpect(jsonPath("$.a", is(10.0)))
                .andExpect(jsonPath("$.b", is(20.0)))
                .andExpect(jsonPath("$.result", is(30.0)));
    }

    @Test
    @DisplayName("GET /api/v1/subtract?a=15&b=5 should return 10.0")
    void testSubtractEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/subtract")
                        .param("a", "15")
                        .param("b", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation", is("subtract")))
                .andExpect(jsonPath("$.a", is(15.0)))
                .andExpect(jsonPath("$.b", is(5.0)))
                .andExpect(jsonPath("$.result", is(10.0)));
    }

    @Test
    @DisplayName("GET /api/v1/multiply?a=4&b=7 should return 28.0")
    void testMultiplyEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/multiply")
                        .param("a", "4")
                        .param("b", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation", is("multiply")))
                .andExpect(jsonPath("$.a", is(4.0)))
                .andExpect(jsonPath("$.b", is(7.0)))
                .andExpect(jsonPath("$.result", is(28.0)));
    }

    @Test
    @DisplayName("GET /api/v1/divide?a=20&b=4 should return 5.0")
    void testDivideEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/divide")
                        .param("a", "20")
                        .param("b", "4")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation", is("divide")))
                .andExpect(jsonPath("$.a", is(20.0)))
                .andExpect(jsonPath("$.b", is(4.0)))
                .andExpect(jsonPath("$.result", is(5.0)));
    }

    @Test
    @DisplayName("GET /api/v1/divide?a=10&b=0 should return HTTP 400 Bad Request with error JSON")
    void testDivideByZeroReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/divide")
                        .param("a", "10")
                        .param("b", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Division by zero is not allowed")))
                .andExpect(jsonPath("$.path", is("/api/v1/divide")));
    }

    @Test
    @DisplayName("GET /api/v1/add with invalid parameter (non-numeric) should return HTTP 400")
    void testInvalidParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "abc")
                        .param("b", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("GET /api/v1/add with missing parameter should return HTTP 400")
    void testMissingParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("GET /api/v1/add?a=NaN should return HTTP 400")
    void testNonFiniteParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "NaN")
                        .param("b", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/power should return HTTP 404 with error JSON")
    void testUnknownEndpointReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/power")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
