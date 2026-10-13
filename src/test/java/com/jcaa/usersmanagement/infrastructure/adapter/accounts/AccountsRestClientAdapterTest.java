package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.BeforeEach;
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
  private static final String USER_ID = "8f684dc4-0c67-4813-8d58-7b5e7f7937b8";

  private MockRestServiceServer server;
  private AccountsRestClientAdapter adapter;

  @BeforeEach
  void setUp() {
    final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    server = MockRestServiceServer.bindTo(builder).build();
    adapter = new AccountsRestClientAdapter(builder.build());
  }

  @Test
  @DisplayName("createAccount() hace POST /api/accounts con el cuerpo acordado")
  void shouldPostAgreedBody() {
    server
        .expect(requestTo(BASE_URL + "/api/accounts"))
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
  @DisplayName("createAccount() lanza AccountCreationException si accounts responde error")
  void shouldThrowWhenAccountsFails() {
    server.expect(requestTo(BASE_URL + "/api/accounts")).andRespond(withServerError());

    final UserModel user = user();
    assertThrows(AccountCreationException.class, () -> adapter.createAccount(user));
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
