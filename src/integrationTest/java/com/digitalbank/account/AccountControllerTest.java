package com.digitalbank.account;

import com.digitalbank.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void shouldCreateAccountAndReturnCreated() throws Exception {
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Guilherme", "initialBalance": 1000.00}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Guilherme"))
            .andExpect(jsonPath("$.balance").value(1000.00))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void shouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "", "initialBalance": 100.00}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAccountById() throws Exception {
        String response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Ana", "initialBalance": 200.00}
                    """))
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/accounts/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ana"));
    }
}
