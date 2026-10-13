package com.jcaa.usersmanagement.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jcaa.usersmanagement.infrastructure.adapter.messaging.KafkaConfig;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaSpringConfig")
class KafkaSpringConfigTest {

  @Test
  @DisplayName("buildJaasConfig() usa ScramLoginModule con usuario y contrasena de Aiven")
  void shouldBuildScramJaasConfig() {
    final KafkaConfig config =
        new KafkaConfig(
            "host:1", "notificaciones", "sasl_ssl", "SCRAM-SHA-256", "avnadmin", "p\"ss", "",
            Duration.ofSeconds(1));

    final String jaas = KafkaSpringConfig.buildJaasConfig(config);

    assertEquals(
        "org.apache.kafka.common.security.scram.ScramLoginModule required "
            + "username=\"avnadmin\" password=\"p\\\"ss\";",
        jaas);
    assertTrue(config.usesSasl());
    assertTrue(config.usesSsl());
  }

  @Test
  @DisplayName("normalizePem() convierte los \\n literales en saltos de linea reales")
  void shouldNormalizePem() {
    assertEquals(
        "-----BEGIN CERTIFICATE-----\nABC\n-----END CERTIFICATE-----",
        KafkaSpringConfig.normalizePem(
            "  -----BEGIN CERTIFICATE-----\\nABC\\n-----END CERTIFICATE-----\n"));
  }

  @Test
  @DisplayName("KafkaConfig exige usuario y contrasena cuando el protocolo es SASL")
  void shouldRequireCredentialsForSasl() {
    final Duration timeout = Duration.ofSeconds(1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new KafkaConfig("host:1", "t", "SASL_SSL", "SCRAM-SHA-256", "", "", "", timeout));
  }
}
