package com.jcaa.usersmanagement.application.port.out;

/** Elimina un archivo. Retorna false si no existia. */
public interface DeleteFilePort {
  boolean delete(String key);
}
