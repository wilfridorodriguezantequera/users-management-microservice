package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.domain.exception.AccountCreationException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Paso 5 del diagrama (sync): users llama a POST /api/accounts del microservicio accounts.
 *
 * <p>Resiliencia (igual que el adaptador de Brevo del profe):
 *
 * <ul>
 *   <li><b>Timeout:</b> cada intento tiene un tiempo maximo de conexion y de lectura.
 *   <li><b>Retry:</b> reintenta con espera exponencial si accounts no responde, responde 5xx,
 *       408 o 429. No reintenta errores del cliente (400, 404...).
 *   <li><b>Circuit breaker:</b> si accounts falla seguido, deja de llamarlo un tiempo para no
 *       saturarlo ni hacer esperar a los usuarios.
 *   <li><b>Fallback:</b> si todo falla se lanza {@link AccountCreationException} y el usuario se
 *       queda en estado PENDING (ver CreateUserService).
 * </ul>
 *
 * <p>Un 409 (Conflict) se toma como exito: significa que la cuenta ya existia, por ejemplo porque
 * un intento anterior si alcanzo a crearla antes de un timeout.
 */
@Slf4j
@Component
public class AccountsRestClientAdapter implements CreateAccountPort {

  static final String CREATE_ACCOUNT_PATH = "/api/accounts";

  private final RestClient restClient;
  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public AccountsRestClientAdapter(
      final AccountsClientConfig config,
      @Qualifier("accountsRestClient") final RestClient restClient) {
    this.restClient = restClient;
    this.retry = buildRetry(config);
    this.circuitBreaker = buildCircuitBreaker(config);
    this.retry
        .getEventPublisher()
        .onRetry(
            event ->
                log.warn(
                    "[AccountsRestClientAdapter] reintento {} hacia accounts tras fallo: {}",
                    event.getNumberOfRetryAttempts(),
                    exceptionType(event.getLastThrowable())));
  }

  @Override
  public void createAccount(final UserModel user) {
    final String userId = user.getId().value();
    final CreateAccountRequest request =
        new CreateAccountRequest(userId, user.getEmail().value(), user.getName().value());
    final Runnable protectedRequest =
        Retry.decorateRunnable(
            retry, CircuitBreaker.decorateRunnable(circuitBreaker, () -> sendRequest(request)));
    try {
      protectedRequest.run();
      log.info("[AccountsRestClientAdapter] cuenta creada en accounts para el usuario {}.", userId);
    } catch (final CallNotPermittedException exception) {
      throw AccountCreationException.becauseCircuitIsOpen(userId);
    } catch (final RestClientResponseException exception) {
      throw AccountCreationException.becauseServiceRejected(
          userId, exception.getStatusCode().value());
    } catch (final RestClientException exception) {
      throw AccountCreationException.becauseServiceUnavailable(userId, exception);
    }
  }

  private void sendRequest(final CreateAccountRequest request) {
    try {
      restClient.post().uri(CREATE_ACCOUNT_PATH).body(request).retrieve().toBodilessEntity();
    } catch (final HttpClientErrorException.Conflict alreadyExists) {
      log.info(
          "[AccountsRestClientAdapter] la cuenta del usuario {} ya existia (409).",
          request.userId());
    }
  }

  private static Retry buildRetry(final AccountsClientConfig config) {
    final IntervalFunction backoff =
        IntervalFunction.ofExponentialBackoff(
            config.retryInitialDelay().toMillis(), config.retryMultiplier());
    final RetryConfig retryConfig =
        RetryConfig.custom()
            .maxAttempts(config.retryMaxAttempts())
            .intervalFunction(backoff)
            .retryOnException(AccountsRestClientAdapter::isTransientFailure)
            .build();
    return Retry.of("accounts-create", retryConfig);
  }

  private static CircuitBreaker buildCircuitBreaker(final AccountsClientConfig config) {
    final CircuitBreakerConfig circuitConfig =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(config.circuitFailureThreshold())
            .slidingWindowSize(config.circuitSlidingWindowSize())
            .minimumNumberOfCalls(config.circuitMinimumCalls())
            .waitDurationInOpenState(config.circuitOpenDuration())
            .recordException(AccountsRestClientAdapter::isTransientFailure)
            .build();
    return CircuitBreaker.of("accounts-create", circuitConfig);
  }

  static boolean isTransientFailure(final Throwable exception) {
    if (exception instanceof ResourceAccessException
        || exception instanceof HttpServerErrorException) {
      return true;
    }
    if (exception instanceof HttpClientErrorException clientError) {
      final int status = clientError.getStatusCode().value();
      return status == HttpStatus.REQUEST_TIMEOUT.value()
          || status == HttpStatus.TOO_MANY_REQUESTS.value();
    }
    return false;
  }

  private static String exceptionType(final Throwable exception) {
    return Objects.isNull(exception) ? "causa desconocida" : exception.getClass().getSimpleName();
  }
}
