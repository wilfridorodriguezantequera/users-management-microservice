package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import java.time.Duration;

/** Configuracion del cliente HTTP hacia accounts y de su resiliencia (timeout, retry, circuit breaker). */
public record AccountsClientConfig(
    String baseUrl,
    Duration connectTimeout,
    Duration readTimeout,
    int retryMaxAttempts,
    Duration retryInitialDelay,
    double retryMultiplier,
    int circuitFailureThreshold,
    int circuitSlidingWindowSize,
    int circuitMinimumCalls,
    Duration circuitOpenDuration) {

  public AccountsClientConfig {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException(
          "La configuracion requerida 'accounts.base-url / ACCOUNTS_URL' no puede estar vacia.");
    }
    baseUrl = baseUrl.trim().replaceAll("/+$", "");
    if (retryMaxAttempts < 1) {
      throw new IllegalArgumentException("accounts.retry.max-attempts debe ser al menos 1.");
    }
  }
}
