package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Paso 2 del diagrama: en vez de enviar el correo directamente con Brevo, users deja el mensaje
 * en el topico de Kafka y el microservicio notifiers se encarga de enviarlo.
 *
 * <p>Implementa el mismo puerto {@link EmailSenderPort} que antes implementaba el adaptador de
 * Brevo, por eso la capa de aplicacion ({@code EmailNotificationService}) no cambia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEmailPublisherAdapter implements EmailSenderPort {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final KafkaConfig config;

  @Override
  public void send(final EmailDestinationModel destination) {
    final String email = destination.getDestinationEmail();
    final String json = toJson(NotificationMessage.from(destination));
    try {
      kafkaTemplate
          .send(config.topic(), email, json)
          .get(config.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
      log.info(
          "[KafkaEmailPublisherAdapter] notificacion para {} publicada en el topico '{}'.",
          email,
          config.topic());
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw EmailSenderException.becausePublishFailed(email, config.topic(), exception);
    } catch (final ExecutionException | TimeoutException | RuntimeException exception) {
      throw EmailSenderException.becausePublishFailed(email, config.topic(), exception);
    }
  }

  private String toJson(final NotificationMessage message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (final JsonProcessingException exception) {
      throw EmailSenderException.becauseSendFailed(exception);
    }
  }
}
