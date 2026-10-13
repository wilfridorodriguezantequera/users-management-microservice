package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.messaging.KafkaConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Productor de Kafka para publicar en el topico 'notificaciones'.
 *
 * <p>El certificado CA de Aiven se pasa como texto PEM en la variable KAFKA_CA_CERT, asi no
 * hay que subir ningun archivo al repositorio ni a Render.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class KafkaSpringConfig {

  private static final int REQUEST_TIMEOUT_MS = 10_000;
  private static final int DELIVERY_TIMEOUT_MS = 20_000;
  private static final int MAX_BLOCK_MS = 10_000;

  @Bean
  public KafkaConfig kafkaConfig(
      @Value("${kafka.bootstrap-servers}") final String bootstrapServers,
      @Value("${kafka.topic}") final String topic,
      @Value("${kafka.security-protocol}") final String securityProtocol,
      @Value("${kafka.sasl.mechanism}") final String saslMechanism,
      @Value("${kafka.username}") final String username,
      @Value("${kafka.password}") final String password,
      @Value("${kafka.ca-cert}") final String caCertificate,
      @Value("${kafka.send-timeout}") final Duration sendTimeout) {
    final KafkaConfig config =
        new KafkaConfig(
            bootstrapServers,
            topic,
            securityProtocol,
            saslMechanism,
            username,
            password,
            caCertificate,
            sendTimeout);
    log.info(
        "[KafkaSpringConfig] configuracion cargada. servers={} topic={} protocol={} caCert={}",
        config.bootstrapServers(),
        config.topic(),
        config.securityProtocol(),
        config.hasCaCertificate() ? "si" : "no");
    return config;
  }

  @Bean
  public ProducerFactory<String, String> producerFactory(final KafkaConfig config) {
    final Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, REQUEST_TIMEOUT_MS);
    props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, DELIVERY_TIMEOUT_MS);
    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, MAX_BLOCK_MS);
    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, config.securityProtocol());

    if (config.usesSasl()) {
      props.put(SaslConfigs.SASL_MECHANISM, config.saslMechanism());
      props.put(SaslConfigs.SASL_JAAS_CONFIG, buildJaasConfig(config));
    }
    if (config.usesSsl() && config.hasCaCertificate()) {
      props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
      props.put(
          SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, normalizePem(config.caCertificate()));
    }
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(
      final ProducerFactory<String, String> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
  }

  static String buildJaasConfig(final KafkaConfig config) {
    final String loginModule =
        config.saslMechanism().toUpperCase(Locale.ROOT).startsWith("SCRAM")
            ? "org.apache.kafka.common.security.scram.ScramLoginModule"
            : "org.apache.kafka.common.security.plain.PlainLoginModule";
    return String.format(
        "%s required username=\"%s\" password=\"%s\";",
        loginModule, escape(config.username()), escape(config.password()));
  }

  /** Render guarda bien los saltos de linea, pero si alguien pega el PEM con "\n" literales se corrigen. */
  static String normalizePem(final String pem) {
    return pem.replace("\\n", "\n").trim();
  }

  private static String escape(final String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
