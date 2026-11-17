package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import java.util.List;

/** Lista los archivos cuya ruta empieza por el prefijo indicado. */
public interface ListFilesPort {
  List<StoredFileModel> list(String prefix);
}
