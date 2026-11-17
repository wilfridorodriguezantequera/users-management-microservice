package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import java.time.Instant;

public record StoredFileRestResponse(
    String key, String name, long size, String contentType, Instant lastModified) {

  public static StoredFileRestResponse from(final StoredFileModel file) {
    return new StoredFileRestResponse(
        file.key(), file.name(), file.size(), file.contentType(), file.lastModified());
  }
}
