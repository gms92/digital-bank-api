package com.digitalbank.transfer;

import com.digitalbank.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    String sourceId;
    String targetId;

    @BeforeEach
    void createAccounts() throws Exception {
        sourceId = createAccount("Alice", "1000.00");
        targetId = createAccount("Bob", "500.00");
    }

    private String createAccount(String name, String balance) throws Exception {
        String response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {"name": "%s", "initialBalance": %s}
                    """, name, balance)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void shouldTransferSuccessfully() throws Exception {
        String transferId = UUID.randomUUID().toString();

        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                      "transferId": "%s",
                      "sourceAccountId": "%s",
                      "targetAccountId": "%s",
                      "amount": 200.00
                    }
                    """, transferId, sourceId, targetId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(transferId));
    }

    @Test
    void shouldReturn422WhenInsufficientBalance() throws Exception {
        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                      "transferId": "%s",
                      "sourceAccountId": "%s",
                      "targetAccountId": "%s",
                      "amount": 99999.00
                    }
                    """, UUID.randomUUID(), sourceId, targetId)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldBeIdempotentWhenSameTransferIdWithSameData() throws Exception {
        String transferId = UUID.randomUUID().toString();
        String body = String.format("""
            {
              "transferId": "%s",
              "sourceAccountId": "%s",
              "targetAccountId": "%s",
              "amount": 100.00
            }
            """, transferId, sourceId, targetId);

        mockMvc.perform(post("/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/transfers").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferId));

        mockMvc.perform(get("/accounts/" + sourceId))
            .andExpect(jsonPath("$.balance").value(900.00));
    }

    @Test
    void shouldReturnConflictWhenSameTransferIdWithDifferentData() throws Exception {
        String transferId = UUID.randomUUID().toString();

        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                      "transferId": "%s",
                      "sourceAccountId": "%s",
                      "targetAccountId": "%s",
                      "amount": 100.00
                    }
                    """, transferId, sourceId, targetId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                      "transferId": "%s",
                      "sourceAccountId": "%s",
                      "targetAccountId": "%s",
                      "amount": 200.00
                    }
                    """, transferId, sourceId, targetId)))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnPaginatedStatements() throws Exception {
        String transferId = UUID.randomUUID().toString();

        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                    {
                      "transferId": "%s",
                      "sourceAccountId": "%s",
                      "targetAccountId": "%s",
                      "amount": 50.00
                    }
                    """, transferId, sourceId, targetId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/" + sourceId + "/statements?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].type").value("DEBIT"));
    }
}
