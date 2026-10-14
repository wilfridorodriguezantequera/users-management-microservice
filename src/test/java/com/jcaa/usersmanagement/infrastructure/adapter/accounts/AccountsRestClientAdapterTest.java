package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.AccountCreationException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("AccountsRestClientAdapter")
class AccountsRestClientAdapterTest {

  private static final String BASE_URL = "http://accounts.test";
  private static final String ACCOUNTS_URL = BASE_URL + "/api/accounts";
  private static final String USER_ID = "8f684dc4-0c67-4813-8d58-7b5e7f7937b8";

  private MockRestServiceServer server;

  private AccountsRestClientAdapter adapter(final int minimumCallsToOpenCircuit) {
    final AccountsClientConfig config =
        new AccountsClientConfig(
            BASE_URL,
            Duration.ofMillis(100),
            Duration.ofMillis(100),
            3,
            Duration.ofMillis(1),
            1.0,
            50,
            minimumCallsToOpenCircuit,
            minimumCallsToOpenCircuit,
            Duration.ofMinutes(1));
    final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    server = MockRestServiceServer.bindTo(builder).build();
    return new AccountsRestClientAdapter(config, builder.build());
  }

  @Test
  @DisplayName("createAccount() hace POST /api/accounts con el cuerpo acordado")
  void shouldPostAgreedBody() {
    final AccountsRestClientAdapter adapter = adapter(10);
    server
        .expect(once(), requestTo(ACCOUNTS_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    "{\"userId\":\"" + USER_ID + "\",\"email\":\"ana@gmail.com\","
                        + "\"nombre\":\"Ana Perez\"}",
                    true))
        .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON));

    adapter.createAccount(user());

    server.verify();
  }

  @Test
  @DisplayName("Retry: si accounts responde 503 reintenta y termina bien al segundo intento")
  void shouldRetryOnServerErrorAndSucceed() {
    final AccountsRestClientAdapter adapter = adapter(10);
    server
        .expect(once(), requestTo(ACCOUNTS_URL))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(once(), requestTo(ACCOUNTS_URL)).andRespond(withStatus(HttpStatus.CREATED));

    assertDoesNotThrow(() -> adapter.createAccount(user()));
    server.verify();
  }

  @Test
  @DisplayName("Retry: tras agotar los 3 intentos lanza AccountCreationException (fallback)")
  void shouldThrowAfterExhaustingRetries() {
    final AccountsRestClientAdapter adapter = adapter(10);
    server.expect(times(3), requestTo(ACCOUNTS_URL)).andRespond(withServerError());

    final UserModel user = user();
    assertThrows(AccountCreationException.class, () -> adapter.createAccount(user));
    server.verify();
  }

  @Test
  @DisplayName("No reintenta errores del cliente como 400")
  void shouldNotRetryClientErrors() {
    final AccountsRestClientAdapter adapter = adapter(10);
    server.expect(once(), requestTo(ACCOUNTS_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

    final UserModel user = user();
    assertThrows(AccountCreationException.class, () -> adapter.createAccount(user));
    server.verify();
  }

  @Test
  @DisplayName("Un 409 significa que la cuenta ya existia y se toma como exito")
  void shouldTreatConflictAsSuccess() {
    final AccountsRestClientAdapter adapter = adapter(10);
    server.expect(once(), requestTo(ACCOUNTS_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

    assertDoesNotThrow(() -> adapter.createAccount(user()));
    server.verify();
  }

  @Test
  @DisplayName("Circuit breaker: tras varios fallos deja de llamar a accounts")
  void shouldOpenCircuitAfterRepeatedFailures() {
    final AccountsRestClientAdapter adapter = adapter(2);
    // 1a llamada: 2 intentos fallidos bastan para abrir el circuito (minimo 2 llamadas, 50 %);
    // el 3er intento ya no sale porque el circuito esta abierto.
    server.expect(times(2), requestTo(ACCOUNTS_URL)).andRespond(withServerError());
    final UserModel user = user();
    assertThrows(AccountCreationException.class, () -> adapter.createAccount(user));

    // 2a llamada: el circuito esta abierto, no se hace ninguna peticion HTTP.
    assertThrows(AccountCreationException.class, () -> adapter.createAccount(user));
    server.verify();
  }

  private static UserModel user() {
    return new UserModel(
        new UserId(USER_ID),
        new UserName("Ana Perez"),
        new UserEmail("ana@gmail.com"),
        UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
        UserRole.MEMBER,
        UserStatus.PENDING);
  }
}
