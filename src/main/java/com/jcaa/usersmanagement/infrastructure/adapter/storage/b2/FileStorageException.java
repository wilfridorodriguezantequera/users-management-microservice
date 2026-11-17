package com.jcaa.usersmanagement.infrastructure.adapter.storage.b2;

/** Error al hablar con Backblaze B2 (red, credenciales, bucket). */
public final class FileStorageException extends RuntimeException {

  private FileStorageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static FileStorageException because(final String operation, final String key,
      final Throwable cause) {
    return new FileStorageException(
        "No se pudo " + operation + " el archivo '" + key + "' en Backblaze B2.", cause);
  }
}
