package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.domain.exception.AccountCreationException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Paso 5 del diagrama (sync): users llama a POST /api/accounts del microservicio accounts y
 * espera la respuesta.
 */
@Slf4j
@Component
public class AccountsRestClientAdapter implements CreateAccountPort {

  static final String CREATE_ACCOUNT_PATH = "/api/accounts";

  private final RestClient restClient;

  public AccountsRestClientAdapter(@Qualifier("accountsRestClient") final RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public void createAccount(final UserModel user) {
    final String userId = user.getId().value();
    final CreateAccountRequest request =
        new CreateAccountRequest(userId, user.getEmail().value(), user.getName().value());
    try {
      restClient.post().uri(CREATE_ACCOUNT_PATH).body(request).retrieve().toBodilessEntity();
      log.info("[AccountsRestClientAdapter] cuenta creada en accounts para el usuario {}.", userId);
    } catch (final RestClientResponseException exception) {
      throw AccountCreationException.becauseServiceRejected(
          userId, exception.getStatusCode().value());
    } catch (final RestClientException exception) {
      throw AccountCreationException.becauseServiceUnavailable(userId, exception);
    }
  }
}
