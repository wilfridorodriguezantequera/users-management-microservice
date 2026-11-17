package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import java.util.List;

/** Guia 9: archivos de cada usuario en Backblaze B2. */
public interface UserFilesUseCase {

  StoredFileModel upload(UploadUserFileCommand command);

  List<StoredFileModel> list(String userId);

  FileContentModel download(String userId, String key);

  void delete(String userId, String key);
}
