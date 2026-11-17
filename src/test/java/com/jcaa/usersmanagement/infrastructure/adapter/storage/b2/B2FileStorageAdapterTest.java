package com.jcaa.usersmanagement.infrastructure.adapter.storage.b2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@DisplayName("B2FileStorageAdapter")
@ExtendWith(MockitoExtension.class)
class B2FileStorageAdapterTest {

  private static final String BUCKET = "electiva-users-files";
  private static final String KEY = "user/u-001/2026/11/18/a.txt";

  @Mock private S3Client s3Client;

  private B2FileStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter =
        new B2FileStorageAdapter(
            s3Client,
            new B2Config("s3.us-east-005.backblazeb2.com", "us-east-005", BUCKET, "id", "key"));
  }

  @Test
  @DisplayName("upload() hace PUT al bucket con la ruta y el tipo")
  void shouldUpload() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    adapter.upload(KEY, "text/plain", "hola".getBytes());

    final ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertEquals(BUCKET, captor.getValue().bucket());
    assertEquals(KEY, captor.getValue().key());
    assertEquals("text/plain", captor.getValue().contentType());
    assertEquals(Long.valueOf(4), captor.getValue().contentLength());
  }

  @Test
  @DisplayName("upload() envuelve los errores de B2")
  void shouldWrapUploadErrors() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(SdkClientException.create("sin red"));

    assertThrows(
        FileStorageException.class, () -> adapter.upload(KEY, "text/plain", new byte[] {1}));
  }

  @Test
  @DisplayName("list() recorre todas las paginas")
  void shouldListAllPages() {
    final Instant date = Instant.parse("2026-11-18T10:00:00Z");
    when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(
            ListObjectsV2Response.builder()
                .contents(S3Object.builder().key(KEY).size(4L).lastModified(date).build())
                .isTruncated(true)
                .nextContinuationToken("next")
                .build(),
            ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("user/u-001/2026/11/18/b.png").size(9L).build())
                .isTruncated(false)
                .build());

    final List<StoredFileModel> files = adapter.list("user/u-001/");

    assertEquals(2, files.size());
    assertEquals("a.txt", files.get(0).name());
    assertEquals(4L, files.get(0).size());
    assertEquals(date, files.get(0).lastModified());
    assertEquals("b.png", files.get(1).name());
  }

  @Test
  @DisplayName("download() retorna nombre, tipo y bytes")
  void shouldDownload() {
    final ResponseBytes<GetObjectResponse> bytes =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().contentType("text/plain").build(), "hola".getBytes());
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);

    final Optional<FileContentModel> file = adapter.download(KEY);

    assertTrue(file.isPresent());
    assertEquals("a.txt", file.get().name());
    assertEquals("text/plain", file.get().contentType());
    assertArrayEquals("hola".getBytes(), file.get().content());
  }

  @Test
  @DisplayName("download() retorna vacio si no existe")
  void shouldReturnEmptyWhenMissing() {
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().statusCode(404).message("no").build());

    assertTrue(adapter.download(KEY).isEmpty());
  }

  @Test
  @DisplayName("delete() borra si existe")
  void shouldDeleteWhenExists() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().build());

    assertTrue(adapter.delete(KEY));
    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("delete() retorna false si no existe")
  void shouldNotDeleteWhenMissing() {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().statusCode(404).message("no").build());

    assertFalse(adapter.delete(KEY));
    verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }
}
