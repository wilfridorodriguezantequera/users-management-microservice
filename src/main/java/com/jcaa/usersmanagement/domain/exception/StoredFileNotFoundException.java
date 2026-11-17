package com.jcaa.usersmanagement.domain.exception;

public final class StoredFileNotFoundException extends DomainException {

  private static final String MESSAGE = "The file '%s' was not found.";

  private StoredFileNotFoundException(final String message) {
    super(message);
  }

  public static StoredFileNotFoundException becauseKeyWasNotFound(final String key) {
    return new StoredFileNotFoundException(String.format(MESSAGE, key));
  }
}
