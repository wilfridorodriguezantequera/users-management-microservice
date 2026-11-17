package com.jcaa.usersmanagement.infrastructure.adapter.storage.b2;

import com.jcaa.usersmanagement.application.port.out.DeleteFilePort;
import com.jcaa.usersmanagement.application.port.out.DownloadFilePort;
import com.jcaa.usersmanagement.application.port.out.ListFilesPort;
import com.jcaa.usersmanagement.application.port.out.UploadFilePort;
import com.jcaa.usersmanagement.domain.model.FileContentModel;
import com.jcaa.usersmanagement.domain.model.StoredFileModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Adaptador de salida: guarda los archivos de los usuarios en el bucket privado de B2. */
@Slf4j
@Component
@RequiredArgsConstructor
public class B2FileStorageAdapter
    implements UploadFilePort, ListFilesPort, DownloadFilePort, DeleteFilePort {

  private static final int NOT_FOUND = 404;

  private final S3Client s3Client;
  private final B2Config config;

  @Override
  public void upload(final String key, final String contentType, final byte[] content) {
    final PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(config.bucket())
            .key(key)
            .contentType(contentType)
            .contentLength((long) content.length)
            .build();
    try {
      s3Client.putObject(request, RequestBody.fromBytes(content));
    } catch (final SdkException exception) {
      throw FileStorageException.because("subir", key, exception);
    }
  }

  @Override
  public List<StoredFileModel> list(final String prefix) {
    final List<StoredFileModel> files = new ArrayList<>();
    String continuationToken = null;
    try {
      do {
        final ListObjectsV2Request request =
            ListObjectsV2Request.builder()
                .bucket(config.bucket())
                .prefix(prefix)
                .continuationToken(continuationToken)
                .build();
        final ListObjectsV2Response response = s3Client.listObjectsV2(request);
        for (final S3Object object : response.contents()) {
          files.add(
              new StoredFileModel(
                  object.key(),
                  nameOf(object.key()),
                  object.size() == null ? 0 : object.size(),
                  null,
                  object.lastModified()));
        }
        continuationToken =
            Boolean.TRUE.equals(response.isTruncated()) ? response.nextContinuationToken() : null;
      } while (continuationToken != null);
    } catch (final SdkException exception) {
      throw FileStorageException.because("listar", prefix, exception);
    }
    return files;
  }

  @Override
  public Optional<FileContentModel> download(final String key) {
    final GetObjectRequest request =
        GetObjectRequest.builder().bucket(config.bucket()).key(key).build();
    try {
      final ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
      return Optional.of(
          new FileContentModel(nameOf(key), bytes.response().contentType(), bytes.asByteArray()));
    } catch (final NoSuchKeyException exception) {
      return Optional.empty();
    } catch (final S3Exception exception) {
      if (exception.statusCode() == NOT_FOUND) {
        return Optional.empty();
      }
      throw FileStorageException.because("descargar", key, exception);
    } catch (final SdkException exception) {
      throw FileStorageException.because("descargar", key, exception);
    }
  }

  @Override
  public boolean delete(final String key) {
    try {
      // S3 no avisa si el archivo no existia; por eso se consulta antes con HEAD.
      s3Client.headObject(HeadObjectRequest.builder().bucket(config.bucket()).key(key).build());
    } catch (final NoSuchKeyException exception) {
      return false;
    } catch (final S3Exception exception) {
      if (exception.statusCode() == NOT_FOUND) {
        return false;
      }
      throw FileStorageException.because("eliminar", key, exception);
    } catch (final SdkException exception) {
      throw FileStorageException.because("eliminar", key, exception);
    }
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(config.bucket()).key(key).build());
      return true;
    } catch (final SdkException exception) {
      throw FileStorageException.because("eliminar", key, exception);
    }
  }

  private static String nameOf(final String key) {
    return key.substring(key.lastIndexOf('/') + 1);
  }
}
