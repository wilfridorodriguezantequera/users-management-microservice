package com.jcaa.usersmanagement.application.port.out;

/** Sube un archivo al almacenamiento de objetos. Si la ruta ya existe, lo reemplaza. */
public interface UploadFilePort {
  void upload(String key, String contentType, byte[] content);
}
