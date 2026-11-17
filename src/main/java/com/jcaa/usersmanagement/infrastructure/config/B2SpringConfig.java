package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.storage.b2.B2Config;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/** Guia 9: cliente S3 (AWS SDK v2) apuntando a Backblaze B2. */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class B2SpringConfig {

  @Bean
  public B2Config b2Config(
      @Value("${b2.endpoint}") final String endpoint,
      @Value("${b2.region}") final String region,
      @Value("${b2.bucket}") final String bucket,
      @Value("${b2.key-id}") final String keyId,
      @Value("${b2.application-key}") final String applicationKey) {
    final B2Config config = new B2Config(endpoint, region, bucket, keyId, applicationKey);
    log.info("[B2SpringConfig] endpoint={} bucket={}", config.endpoint(), config.bucket());
    return config;
  }

  @Bean(destroyMethod = "close")
  public S3Client b2S3Client(final B2Config config) {
    return S3Client.builder()
        .endpointOverride(URI.create(config.endpoint()))
        .region(Region.of(config.region()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.keyId(), config.applicationKey())))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
