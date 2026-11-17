package com.jcaa.usersmanagement.domain.exception;

public final class InvalidFileException extends DomainException {

  private static final String MESSAGE_EMPTY = "The file must not be empty.";
  private static final String MESSAGE_NAME = "The file name '%s' is not valid.";
  private static final String MESSAGE_KEY = "The file key '%s' does not belong to user '%s'.";

  private InvalidFileException(final String message) {
    super(message);
  }

  public static InvalidFileException becauseFileIsEmpty() {
    return new InvalidFileException(MESSAGE_EMPTY);
  }

  public static InvalidFileException becauseNameIsInvalid(final String name) {
    return new InvalidFileException(String.format(MESSAGE_NAME, name));
  }

  public static InvalidFileException becauseKeyDoesNotBelongToUser(
      final String key, final String userId) {
    return new InvalidFileException(String.format(MESSAGE_KEY, key, userId));
  }
}
