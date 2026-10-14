package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.accounts.AccountsClientConfig;
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
 * Cliente HTTP hacia el microservicio accounts (paso 5, sincrono).
 *
 * <p>Timeout: cada intento espera como maximo ACCOUNTS_READ_TIMEOUT. En el plan gratis de Render
 * accounts puede estar "dormido": el primer intento lo despierta y los reintentos lo alcanzan.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AccountsSpringConfig {

  @Bean
  public AccountsClientConfig accountsClientConfig(
      @Value("${accounts.base-url}") final String baseUrl,
      @Value("${accounts.timeout.connect}") final Duration connectTimeout,
      @Value("${accounts.timeout.read}") final Duration readTimeout,
      @Value("${accounts.retry.max-attempts}") final int retryMaxAttempts,
      @Value("${accounts.retry.initial-delay}") final Duration retryInitialDelay,
      @Value("${accounts.retry.multiplier}") final double retryMultiplier,
      @Value("${accounts.circuit-breaker.failure-rate-threshold}") final int circuitFailureThreshold,
      @Value("${accounts.circuit-breaker.sliding-window-size}") final int circuitSlidingWindowSize,
      @Value("${accounts.circuit-breaker.minimum-calls}") final int circuitMinimumCalls,
      @Value("${accounts.circuit-breaker.open-duration}") final Duration circuitOpenDuration) {
    final AccountsClientConfig config =
        new AccountsClientConfig(
            baseUrl,
            connectTimeout,
            readTimeout,
            retryMaxAttempts,
            retryInitialDelay,
            retryMultiplier,
            circuitFailureThreshold,
            circuitSlidingWindowSize,
            circuitMinimumCalls,
            circuitOpenDuration);
    log.info(
        "[AccountsSpringConfig] accounts.base-url={} readTimeout={} reintentos={}",
        config.baseUrl(),
        config.readTimeout(),
        config.retryMaxAttempts());
    return config;
  }

  @Bean(name = "accountsRestClient")
  public RestClient accountsRestClient(final AccountsClientConfig config) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(config.connectTimeout());
    requestFactory.setReadTimeout(config.readTimeout());
    return RestClient.builder()
        .baseUrl(config.baseUrl())
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
