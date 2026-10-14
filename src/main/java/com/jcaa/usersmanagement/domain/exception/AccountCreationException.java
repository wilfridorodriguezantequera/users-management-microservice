package com.jcaa.usersmanagement.domain.exception;

public final class AccountCreationException extends DomainException {

  private static final String REJECTED_MESSAGE =
      "El servicio accounts rechazo la creacion de la cuenta del usuario '%s' (HTTP %d).";
  private static final String CIRCUIT_OPEN_MESSAGE =
      "El circuito hacia accounts esta abierto por fallos recientes; no se creo la cuenta del usuario '%s'.";
  private static final String UNAVAILABLE_MESSAGE =
      "No se pudo contactar al servicio accounts para crear la cuenta del usuario '%s'.";

  private AccountCreationException(final String message) {
    super(message);
  }

  private AccountCreationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static AccountCreationException becauseServiceRejected(
      final String userId, final int httpStatus) {
    return new AccountCreationException(String.format(REJECTED_MESSAGE, userId, httpStatus));
  }

  public static AccountCreationException becauseServiceUnavailable(
      final String userId, final Throwable cause) {
    return new AccountCreationException(String.format(UNAVAILABLE_MESSAGE, userId), cause);
  }

  public static AccountCreationException becauseCircuitIsOpen(final String userId) {
    return new AccountCreationException(String.format(CIRCUIT_OPEN_MESSAGE, userId));
  }
}
