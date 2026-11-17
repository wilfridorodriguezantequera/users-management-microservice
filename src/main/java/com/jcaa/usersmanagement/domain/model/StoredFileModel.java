package com.jcaa.usersmanagement.domain.model;

import java.time.Instant;

/**
 * Archivo guardado en el almacenamiento de objetos (Backblaze B2).
 *
 * @param key ruta completa del objeto: user/{id}/{anio}/{mes}/{dia}/{archivo}
 * @param name nombre del archivo (ultima parte de la ruta)
 * @param size tamano en bytes
 * @param contentType tipo MIME (puede ser null al listar)
 * @param lastModified fecha de la ultima modificacion
 */
public record StoredFileModel(
    String key, String name, long size, String contentType, Instant lastModified) {}
