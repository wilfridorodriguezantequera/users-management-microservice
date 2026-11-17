package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.UserFilesUseCase;
import com.jcaa.usersmanagement.application.port.out.DeleteFilePort;
import com.jcaa.usersmanagement.application.port.out.DownloadFilePort;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ListFilesPort;
import com.jcaa.usersmanagement.application.port.out.UploadFilePort;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.exception.InvalidFileException;
import com.jcaa.usersmanagement.domain.exception.StoredFileNotFoundException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Guia 9: cada usuario tiene sus archivos en el bucket privado de Backblaze B2, organizados por
 * fecha de carga: {@code user/{id}/{anio}/{mes}/{dia}/{archivo}}.
 */
@Slf4j
@Service
public class UserFilesService implements UserFilesUseCase {

  static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final int MAX_NAME_LENGTH = 150;

  private final GetUserByIdPort getUserByIdPort;
  private final UploadFilePort uploadFilePort;
  private final ListFilesPort listFilesPort;
  private final DownloadFilePort downloadFilePort;
  private final DeleteFilePort deleteFilePort;
  private final Clock clock;

  @Autowired
  public UserFilesService(
      final GetUserByIdPort getUserByIdPort,
      final UploadFilePort uploadFilePort,
      final ListFilesPort listFilesPort,
      final DownloadFilePort downloadFilePort,
      final DeleteFilePort deleteFilePort) {
    this(
        getUserByIdPort,
        uploadFilePort,
        listFilesPort,
        downloadFilePort,
        deleteFilePort,
        Clock.systemUTC());
  }

  UserFilesService(
      final GetUserByIdPort getUserByIdPort,
      final UploadFilePort uploadFilePort,
      final ListFilesPort listFilesPort,
      final DownloadFilePort downloadFilePort,
      final DeleteFilePort deleteFilePort,
      final Clock clock) {
    this.getUserByIdPort = getUserByIdPort;
    this.uploadFilePort = uploadFilePort;
    this.listFilesPort = listFilesPort;
    this.downloadFilePort = downloadFilePort;
    this.deleteFilePort = deleteFilePort;
    this.clock = clock;
  }

  @Override
  public StoredFileModel upload(final UploadUserFileCommand command) {
    final UserId userId = requireExistingUser(command.userId());
    if (command.content() == null || command.content().length == 0) {
      throw InvalidFileException.becauseFileIsEmpty();
    }
    final String name = sanitizeFileName(command.fileName());
    final String contentType =
        command.contentType() == null || command.contentType().isBlank()
            ? DEFAULT_CONTENT_TYPE
            : command.contentType();
    final LocalDate today = LocalDate.now(clock);
    final String key =
        String.format(
            "%s%04d/%02d/%02d/%s",
            prefixOf(userId),
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth(),
            name);

    uploadFilePort.upload(key, contentType, command.content());
    log.info("[UserFilesService] Archivo subido a B2: {} ({} bytes)", key, command.content().length);
    return new StoredFileModel(key, name, command.content().length, contentType, clock.instant());
  }

  @Override
  public List<StoredFileModel> list(final String userId) {
    final UserId id = requireExistingUser(userId);
    return listFilesPort.list(prefixOf(id));
  }

  @Override
  public FileContentModel download(final String userId, final String key) {
    final UserId id = requireExistingUser(userId);
    requireKeyOfUser(id, key);
    return downloadFilePort
        .download(key)
        .orElseThrow(() -> StoredFileNotFoundException.becauseKeyWasNotFound(key));
  }

  @Override
  public void delete(final String userId, final String key) {
    final UserId id = requireExistingUser(userId);
    requireKeyOfUser(id, key);
    if (!deleteFilePort.delete(key)) {
      throw StoredFileNotFoundException.becauseKeyWasNotFound(key);
    }
    log.info("[UserFilesService] Archivo eliminado de B2: {}", key);
  }

  static String prefixOf(final UserId userId) {
    return "user/" + userId.value() + "/";
  }

  /** Deja solo el nombre (sin carpetas) y cambia los caracteres raros por '_'. */
  static String sanitizeFileName(final String original) {
    if (original == null) {
      throw InvalidFileException.becauseNameIsInvalid(null);
    }
    String name = original.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).trim();
    name = name.replaceAll("[^A-Za-z0-9._-]", "_");
    if (name.isEmpty() || name.chars().allMatch(c -> c == '.')) {
      throw InvalidFileException.becauseNameIsInvalid(original);
    }
    if (name.length() > MAX_NAME_LENGTH) {
      name = name.substring(name.length() - MAX_NAME_LENGTH);
    }
    return name;
  }

  private UserId requireExistingUser(final String rawUserId) {
    final UserId userId = new UserId(rawUserId);
    if (getUserByIdPort.getById(userId).isEmpty()) {
      throw UserNotFoundException.becauseIdWasNotFound(userId.value());
    }
    return userId;
  }

  private static void requireKeyOfUser(final UserId userId, final String key) {
    if (key == null || !key.startsWith(prefixOf(userId)) || key.contains("..")) {
      throw InvalidFileException.becauseKeyDoesNotBelongToUser(key, userId.value());
    }
  }
}
