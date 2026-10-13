package com.jcaa.usersmanagement.infrastructure.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia el microservicio accounts.
 *
 * <p>El timeout de lectura es largo a proposito: en el plan gratuito de Render, si accounts esta
 * "dormido" tarda casi un minuto en despertar y responder.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AccountsSpringConfig {

  @Bean(name = "accountsRestClient")
  public RestClient accountsRestClient(
      @Value("${accounts.base-url}") final String baseUrl,
      @Value("${accounts.timeout.connect}") final Duration connectTimeout,
      @Value("${accounts.timeout.read}") final Duration readTimeout) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException(
          "La configuracion requerida 'accounts.base-url / ACCOUNTS_URL' no puede estar vacia.");
    }
    final String normalizedBaseUrl = baseUrl.trim().replaceAll("/+$", "");
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    log.info("[AccountsSpringConfig] accounts.base-url={}", normalizedBaseUrl);
    return RestClient.builder()
        .baseUrl(normalizedBaseUrl)
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
