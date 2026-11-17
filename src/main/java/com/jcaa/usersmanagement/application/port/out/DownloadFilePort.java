package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.FileContentModel;
import java.util.Optional;

/** Descarga un archivo. Retorna vacio si no existe. */
public interface DownloadFilePort {
  Optional<FileContentModel> download(String key);
}
