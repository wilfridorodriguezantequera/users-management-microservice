package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.UserFilesUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.StoredFileRestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guia 9: archivos de cada usuario en Backblaze B2.
 *
 * <p>Solo el mismo usuario (segun su token) o un ADMIN pueden usar los archivos de un usuario.
 */
@RestController
@RequestMapping("/api/users/{id}/files")
@RequiredArgsConstructor
@Tag(name = "Archivos de usuario (Backblaze B2)")
@SecurityRequirement(name = "bearerAuth")
public class UserFileRestController {

  private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

  private final UserFilesUseCase userFilesUseCase;

  @Operation(summary = "Sube un archivo del usuario")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public StoredFileRestResponse upload(
      @PathVariable final String id, @RequestPart("file") final MultipartFile file) {
    requireOwnerOrAdmin(id);
    final UploadUserFileCommand command =
        new UploadUserFileCommand(id, file.getOriginalFilename(), file.getContentType(), bytesOf(file));
    return StoredFileRestResponse.from(userFilesUseCase.upload(command));
  }

  @Operation(summary = "Lista los archivos del usuario")
  @GetMapping
  public List<StoredFileRestResponse> list(@PathVariable final String id) {
    requireOwnerOrAdmin(id);
    return userFilesUseCase.list(id).stream().map(StoredFileRestResponse::from).toList();
  }

  @Operation(summary = "Descarga un archivo del usuario (key = ruta que devuelve el listado)")
  @GetMapping("/download")
  public ResponseEntity<byte[]> download(
      @PathVariable final String id, @RequestParam("key") final String key) {
    requireOwnerOrAdmin(id);
    final FileContentModel file = userFilesUseCase.download(id, key);
    final MediaType mediaType =
        file.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(file.contentType());
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.name()).build().toString())
        .contentLength(file.size())
        .body(file.content());
  }

  @Operation(summary = "Elimina un archivo del usuario (key = ruta que devuelve el listado)")
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable final String id, @RequestParam("key") final String key) {
    requireOwnerOrAdmin(id);
    userFilesUseCase.delete(id, key);
  }

  private static void requireOwnerOrAdmin(final String userId) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final boolean isOwner = authentication != null && userId.equals(authentication.getName());
    final boolean isAdmin =
        authentication != null
            && authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    if (!isOwner && !isAdmin) {
      throw new AccessDeniedException("Solo el mismo usuario o un ADMIN pueden usar estos archivos.");
    }
  }

  private static byte[] bytesOf(final MultipartFile file) {
    try {
      return file.getBytes();
    } catch (final IOException exception) {
      throw new UncheckedIOException("No se pudo leer el archivo recibido.", exception);
    }
  }
}
