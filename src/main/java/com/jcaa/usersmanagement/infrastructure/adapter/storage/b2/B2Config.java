package com.jcaa.usersmanagement.infrastructure.adapter.storage.b2;

/**
 * Configuracion de Backblaze B2 (API compatible con S3).
 *
 * <p>keyId = accessKeyId y applicationKey = secretAccessKey. El endpoint y la region salen de la
 * tarjeta del bucket, por ejemplo {@code s3.us-east-005.backblazeb2.com} y {@code us-east-005}.
 */
public record B2Config(
    String endpoint, String region, String bucket, String keyId, String applicationKey) {

  public B2Config {
    requireText(endpoint, "b2.endpoint / B2_ENDPOINT");
    requireText(region, "b2.region / B2_REGION");
    requireText(bucket, "b2.bucket / B2_BUCKET");
    requireText(keyId, "b2.key-id / B2_KEY_ID");
    requireText(applicationKey, "b2.application-key / B2_APPLICATION_KEY");
    endpoint = endpoint.trim().replaceAll("/+$", "");
    if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
      endpoint = "https://" + endpoint;
    }
    region = region.trim();
    bucket = bucket.trim();
    keyId = keyId.trim();
    applicationKey = applicationKey.trim();
  }

  private static void requireText(final String value, final String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "La configuracion requerida '" + name + "' no puede estar vacia.");
    }
  }

  @Override
  public String toString() {
    return "B2Config[endpoint=" + endpoint + ", region=" + region + ", bucket=" + bucket + "]";
  }
}
