package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;

/** Paso 5 del diagrama: pedirle al microservicio accounts que cree la cuenta del usuario. */
public interface CreateAccountPort {
  void createAccount(UserModel user);
}
