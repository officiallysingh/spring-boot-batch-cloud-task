package com.ksoot.batch.job.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Valid
@ConfigurationProperties(prefix = "application.batch")
public class BatchProperties {

  @Min(1)
  @Max(10000)
  private int chunkSize = 100;

  @Min(1)
  private int skipLimit = 10;

  @Min(1)
  @Max(10)
  private int maxRetries = 3;

  private Duration backoffInitialDelay = Duration.ofSeconds(3);

  @Min(1)
  @Max(5)
  private int backoffMultiplier = 2;

  @Min(1)
  @Max(10000)
  private int itemReaderPageSize = 100;

  @Min(1)
  @Max(128)
  private int partitionSize = 8;

  @Min(1)
  private int triggerPartitioningThreshold = 100;
}
