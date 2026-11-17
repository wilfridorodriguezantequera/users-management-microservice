package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.application.port.out.DeleteFilePort;
import com.jcaa.usersmanagement.application.port.out.DownloadFilePort;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ListFilesPort;
import com.jcaa.usersmanagement.application.port.out.UploadFilePort;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.InvalidFileException;
import com.jcaa.usersmanagement.domain.exception.StoredFileNotFoundException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserFilesService (Guia 9 - Backblaze B2)")
@ExtendWith(MockitoExtension.class)
class UserFilesServiceTest {

  private static final String USER_ID = "u-001";
  private static final Instant NOW = Instant.parse("2026-11-18T10:15:30Z");

  @Mock private GetUserByIdPort getUserByIdPort;
  @Mock private UploadFilePort uploadFilePort;
  @Mock private ListFilesPort listFilesPort;
  @Mock private DownloadFilePort downloadFilePort;
  @Mock private DeleteFilePort deleteFilePort;

  private UserFilesService service;

  @BeforeEach
  void setUp() {
    service =
        new UserFilesService(
            getUserByIdPort,
            uploadFilePort,
            listFilesPort,
            downloadFilePort,
            deleteFilePort,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void userExists() {
    when(getUserByIdPort.getById(new UserId(USER_ID)))
        .thenReturn(
            Optional.of(
                new UserModel(
                    new UserId(USER_ID),
                    new UserName("Ada Lovelace"),
                    new UserEmail("ada@example.com"),
                    UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
                    UserRole.MEMBER,
                    UserStatus.ACTIVE)));
  }

  @Test
  @DisplayName("upload() guarda en user/{id}/{anio}/{mes}/{dia}/{archivo}")
  void shouldUploadWithDatePath() {
    userExists();
    final byte[] content = "hola".getBytes();

    final StoredFileModel result =
        service.upload(new UploadUserFileCommand(USER_ID, "foto perfil.png", "image/png", content));

    final String expectedKey = "user/u-001/2026/11/18/foto_perfil.png";
    verify(uploadFilePort).upload(expectedKey, "image/png", content);
    assertEquals(expectedKey, result.key());
    assertEquals("foto_perfil.png", result.name());
    assertEquals(4, result.size());
    assertEquals(NOW, result.lastModified());
  }

  @Test
  @DisplayName("upload() usa application/octet-stream si no llega el tipo")
  void shouldUseDefaultContentType() {
    userExists();
    final byte[] content = {1, 2, 3};

    service.upload(new UploadUserFileCommand(USER_ID, "datos.bin", null, content));

    verify(uploadFilePort)
        .upload("user/u-001/2026/11/18/datos.bin", UserFilesService.DEFAULT_CONTENT_TYPE, content);
  }

  @Test
  @DisplayName("upload() rechaza un archivo vacio")
  void shouldRejectEmptyFile() {
    userExists();
    assertThrows(
        InvalidFileException.class,
        () -> service.upload(new UploadUserFileCommand(USER_ID, "a.txt", "text/plain", new byte[0])));
    verify(uploadFilePort, never()).upload(anyString(), anyString(), any());
  }

  @Test
  @DisplayName("upload() falla si el usuario no existe")
  void shouldFailWhenUserDoesNotExist() {
    when(getUserByIdPort.getById(new UserId(USER_ID))).thenReturn(Optional.empty());
    assertThrows(
        UserNotFoundException.class,
        () -> service.upload(new UploadUserFileCommand(USER_ID, "a.txt", "text/plain", new byte[] {1})));
  }

  @Test
  @DisplayName("sanitizeFileName() quita carpetas y caracteres raros")
  void shouldSanitizeFileName() {
    assertEquals("informe_final.pdf", UserFilesService.sanitizeFileName("C:\\docs\\informe final.pdf"));
    assertEquals("passwd", UserFilesService.sanitizeFileName("../../etc/passwd"));
    assertThrows(InvalidFileException.class, () -> UserFilesService.sanitizeFileName(".."));
    assertThrows(InvalidFileException.class, () -> UserFilesService.sanitizeFileName("  "));
    assertThrows(InvalidFileException.class, () -> UserFilesService.sanitizeFileName(null));
  }

  @Test
  @DisplayName("list() busca solo con el prefijo del usuario")
  void shouldListByUserPrefix() {
    userExists();
    final StoredFileModel file =
        new StoredFileModel("user/u-001/2026/11/18/a.txt", "a.txt", 1, null, NOW);
    when(listFilesPort.list("user/u-001/")).thenReturn(List.of(file));

    assertEquals(List.of(file), service.list(USER_ID));
  }

  @Test
  @DisplayName("download() retorna el contenido del archivo")
  void shouldDownload() {
    userExists();
    final String key = "user/u-001/2026/11/18/a.txt";
    final FileContentModel file = new FileContentModel("a.txt", "text/plain", new byte[] {7});
    when(downloadFilePort.download(key)).thenReturn(Optional.of(file));

    assertArrayEquals(new byte[] {7}, service.download(USER_ID, key).content());
  }

  @Test
  @DisplayName("download() lanza 404 si el archivo no existe")
  void shouldFailDownloadWhenMissing() {
    userExists();
    final String key = "user/u-001/2026/11/18/a.txt";
    when(downloadFilePort.download(key)).thenReturn(Optional.empty());

    assertThrows(StoredFileNotFoundException.class, () -> service.download(USER_ID, key));
  }

  @Test
  @DisplayName("download() no deja leer archivos de otro usuario")
  void shouldRejectKeyOfAnotherUser() {
    userExists();
    assertThrows(
        InvalidFileException.class,
        () -> service.download(USER_ID, "user/u-002/2026/11/18/a.txt"));
    assertThrows(
        InvalidFileException.class,
        () -> service.download(USER_ID, "user/u-001/../u-002/a.txt"));
    verify(downloadFilePort, never()).download(anyString());
  }

  @Test
  @DisplayName("delete() elimina el archivo")
  void shouldDelete() {
    userExists();
    final String key = "user/u-001/2026/11/18/a.txt";
    when(deleteFilePort.delete(key)).thenReturn(true);

    service.delete(USER_ID, key);

    verify(deleteFilePort).delete(key);
  }

  @Test
  @DisplayName("delete() lanza 404 si el archivo no existia")
  void shouldFailDeleteWhenMissing() {
    userExists();
    final String key = "user/u-001/2026/11/18/a.txt";
    when(deleteFilePort.delete(key)).thenReturn(false);

    assertThrows(StoredFileNotFoundException.class, () -> service.delete(USER_ID, key));
  }
}
