package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@DisplayName("KafkaEmailPublisherAdapter")
@ExtendWith(MockitoExtension.class)
class KafkaEmailPublisherAdapterTest {

  private static final String TOPIC = "notificaciones";

  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private KafkaEmailPublisherAdapter adapter;

  @BeforeEach
  void setUp() {
    final KafkaConfig config =
        new KafkaConfig(
            "localhost:9092", TOPIC, "PLAINTEXT", "SCRAM-SHA-256", "", "", "", Duration.ofSeconds(1));
    adapter = new KafkaEmailPublisherAdapter(kafkaTemplate, objectMapper, config);
  }

  @Test
  @DisplayName("send() publica en el topico el JSON con los campos acordados por el grupo")
  void shouldPublishAgreedJsonFormat() throws Exception {
    final CompletableFuture<SendResult<String, String>> ok = CompletableFuture.completedFuture(null);
    when(kafkaTemplate.send(eq(TOPIC), eq("ana@gmail.com"), anyString())).thenReturn(ok);

    adapter.send(
        new EmailDestinationModel(
            "ana@gmail.com", "Ana Pérez", "Bienvenida", "<p>Hola Ana</p>"));

    final ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplate).send(eq(TOPIC), eq("ana@gmail.com"), json.capture());
    final JsonNode node = objectMapper.readTree(json.getValue());
    assertEquals(4, node.size(), "solo los 4 campos acordados");
    assertEquals("ana@gmail.com", node.get("email").asText());
    assertEquals("Ana Pérez", node.get("nombre").asText());
    assertEquals("Bienvenida", node.get("asunto").asText());
    assertEquals("<p>Hola Ana</p>", node.get("mensaje").asText());
  }

  @Test
  @DisplayName("send() lanza EmailSenderException si Kafka no confirma el mensaje")
  void shouldThrowWhenKafkaFails() {
    final CompletableFuture<SendResult<String, String>> failed =
        CompletableFuture.failedFuture(new IllegalStateException("broker caido"));
    when(kafkaTemplate.send(eq(TOPIC), anyString(), anyString())).thenReturn(failed);

    final EmailDestinationModel destination =
        new EmailDestinationModel("ana@gmail.com", "Ana", "Asunto", "<p>x</p>");

    assertThrows(EmailSenderException.class, () -> adapter.send(destination));
  }
}
