package com.jcaa.usersmanagement.infrastructure.adapter.messaging;

import java.time.Duration;
import java.util.Locale;

/**
 * Configuracion de la conexion a Kafka (Aiven).
 *
 * <p>En Aiven se usa {@code SASL_SSL} con usuario/contrasena y el certificado CA del servicio
 * en formato PEM. En local se puede usar {@code PLAINTEXT} sin credenciales.
 */
public record KafkaConfig(
    String bootstrapServers,
    String topic,
    String securityProtocol,
    String saslMechanism,
    String username,
    String password,
    String caCertificate,
    Duration sendTimeout) {

  public KafkaConfig {
    requireText(bootstrapServers, "kafka.bootstrap-servers / KAFKA_BOOTSTRAP_SERVERS");
    requireText(topic, "kafka.topic / KAFKA_TOPIC");
    requireText(securityProtocol, "kafka.security-protocol / KAFKA_SECURITY_PROTOCOL");
    securityProtocol = securityProtocol.trim().toUpperCase(Locale.ROOT);
    if (usesSasl(securityProtocol)) {
      requireText(saslMechanism, "kafka.sasl.mechanism / KAFKA_SASL_MECHANISM");
      requireText(username, "kafka.username / KAFKA_USERNAME");
      requireText(password, "kafka.password / KAFKA_PASSWORD");
    }
  }

  public boolean usesSasl() {
    return usesSasl(securityProtocol);
  }

  public boolean usesSsl() {
    return securityProtocol.endsWith("SSL");
  }

  public boolean hasCaCertificate() {
    return caCertificate != null && !caCertificate.isBlank();
  }

  private static boolean usesSasl(final String protocol) {
    return protocol.startsWith("SASL");
  }

  private static void requireText(final String value, final String propertyName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "La configuracion requerida '" + propertyName + "' no puede estar vacia.");
    }
  }
}
