package com.jcaa.usersmanagement.infrastructure.adapter.storage.b2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("B2Config")
class B2ConfigTest {

  @Test
  @DisplayName("agrega https:// al endpoint y quita la barra final")
  void shouldNormalizeEndpoint() {
    final B2Config config =
        new B2Config("s3.us-east-005.backblazeb2.com/", "us-east-005", "bucket", "id", "key");
    assertEquals("https://s3.us-east-005.backblazeb2.com", config.endpoint());
  }

  @Test
  @DisplayName("exige las llaves")
  void shouldRequireKeys() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new B2Config("https://s3.x.com", "r", "b", "", "key"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new B2Config("https://s3.x.com", "r", "b", "id", null));
  }

  @Test
  @DisplayName("toString() no muestra las llaves")
  void shouldNotPrintSecrets() {
    final B2Config config = new B2Config("https://s3.x.com", "r", "b", "mi-id", "mi-secreto");
    assertFalse(config.toString().contains("mi-secreto"));
    assertFalse(config.toString().contains("mi-id"));
  }
}
