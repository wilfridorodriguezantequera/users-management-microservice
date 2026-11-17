package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.UserFilesUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtPrincipal;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserFileRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  RestAccessDeniedHandler.class
})
@DisplayName("UserFileRestController (Guia 9)")
class UserFileRestControllerTest {

  private static final String ID = "8f684dc4-0c67-4813-8d58-7b5e7f7937b8";
  private static final String OTHER_ID = "11111111-2222-3333-4444-555555555555";
  private static final String KEY = "user/" + ID + "/2026/11/18/a.txt";

  @Autowired private MockMvc mockMvc;

  @MockBean private UserFilesUseCase userFilesUseCase;
  @MockBean private JwtTokenService jwtTokenService;

  private void tokenFor(final String userId, final UserRole role) {
    when(jwtTokenService.parse("valid-token")).thenReturn(new JwtPrincipal(userId, role));
  }

  @Test
  @DisplayName("POST sube el archivo del mismo usuario y responde 201")
  void shouldUploadOwnFile() throws Exception {
    tokenFor(ID, UserRole.MEMBER);
    when(userFilesUseCase.upload(any(UploadUserFileCommand.class)))
        .thenReturn(new StoredFileModel(KEY, "a.txt", 4, "text/plain", Instant.EPOCH));

    mockMvc
        .perform(
            multipart("/api/users/{id}/files", ID)
                .file(new MockMultipartFile("file", "a.txt", "text/plain", "hola".getBytes()))
                .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.key").value(KEY))
        .andExpect(jsonPath("$.name").value("a.txt"))
        .andExpect(jsonPath("$.size").value(4));
  }

  @Test
  @DisplayName("GET lista los archivos cuando es ADMIN")
  void shouldListAsAdmin() throws Exception {
    tokenFor(OTHER_ID, UserRole.ADMIN);
    when(userFilesUseCase.list(ID))
        .thenReturn(List.of(new StoredFileModel(KEY, "a.txt", 4, null, Instant.EPOCH)));

    mockMvc
        .perform(get("/api/users/{id}/files", ID).header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].key").value(KEY));
  }

  @Test
  @DisplayName("GET responde 403 si es otro usuario que no es ADMIN")
  void shouldForbidOtherUsers() throws Exception {
    tokenFor(OTHER_ID, UserRole.MEMBER);

    mockMvc
        .perform(get("/api/users/{id}/files", ID).header("Authorization", "Bearer valid-token"))
        .andExpect(status().isForbidden());
    verify(userFilesUseCase, never()).list(any());
  }

  @Test
  @DisplayName("GET responde 401 sin token")
  void shouldRequireToken() throws Exception {
    mockMvc.perform(get("/api/users/{id}/files", ID)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /download devuelve los bytes como adjunto")
  void shouldDownload() throws Exception {
    tokenFor(ID, UserRole.MEMBER);
    when(userFilesUseCase.download(ID, KEY))
        .thenReturn(new FileContentModel("a.txt", "text/plain", "hola".getBytes()));

    mockMvc
        .perform(
            get("/api/users/{id}/files/download", ID)
                .param("key", KEY)
                .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"a.txt\""))
        .andExpect(content().bytes("hola".getBytes()));
  }

  @Test
  @DisplayName("DELETE elimina y responde 204")
  void shouldDelete() throws Exception {
    tokenFor(ID, UserRole.MEMBER);

    mockMvc
        .perform(
            delete("/api/users/{id}/files", ID)
                .param("key", KEY)
                .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isNoContent());
    verify(userFilesUseCase).delete(ID, KEY);
  }
}
