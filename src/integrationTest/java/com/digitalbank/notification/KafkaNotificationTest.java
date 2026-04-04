package com.digitalbank.notification;

import com.digitalbank.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {"MAIL_USERNAME=test@test.com", "MAIL_PASSWORD=testpassword"})
class KafkaNotificationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired JavaMailSender mailSender;
    @Autowired ObjectMapper objectMapper;

    @Test
    void shouldSendEmailWhenTransferFundCompleted() throws Exception {
        String sourceId = createAccount("Alice", "1000.00");
        String targetId = createAccount("Bob", "500.00");
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

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, atLeastOnce()).send(captor.capture());

            SimpleMailMessage sentMessage = captor.getValue();
            assertThat(sentMessage.getSubject()).contains(transferId);
            assertThat(sentMessage.getText()).contains(transferId);
        });
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
}
