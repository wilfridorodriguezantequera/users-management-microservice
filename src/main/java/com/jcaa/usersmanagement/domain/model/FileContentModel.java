package com.jcaa.usersmanagement.domain.model;

/** Contenido de un archivo descargado del almacenamiento de objetos. */
public record FileContentModel(String name, String contentType, byte[] content) {

  public long size() {
    return content == null ? 0 : content.length;
  }
}
