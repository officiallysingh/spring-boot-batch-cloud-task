package com.ksoot.batch.job.common;

import static com.ksoot.batch.domain.AppConstants.RUN_ID_SEQUENCE_NAME;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.support.DataFieldMaxValueJobParametersIncrementer;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.BackOffPolicyBuilder;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration // Change this annotation to @AutoConfiguration when this class moved to lib
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(value = {BatchProperties.class})
@RequiredArgsConstructor
public class BatchConfiguration extends DefaultBatchConfiguration {

  //    Define Async Task Executor when executing the jobs from Rest API.
  //  private final TaskExecutor taskExecutor;
  //
  //  @Override
  //  public TaskExecutor getTaskExecutor() {
  //    return this.taskExecutor;
  //  }
  final DataSource dataSource;

  @ConditionalOnMissingBean
  @Bean
  JobParametersIncrementer jobParametersIncrementer(final DataSource dataSource) {
    return new DataFieldMaxValueJobParametersIncrementer(
        new PostgresSequenceMaxValueIncrementer(dataSource, RUN_ID_SEQUENCE_NAME));
  }

  //  @Bean
  public PlatformTransactionManager getTransactionManager() {
    return new DataSourceTransactionManager(this.dataSource);
  }

  @ConditionalOnMissingBean
  @Bean
  BackOffPolicy backOffPolicy(final BatchProperties batchProperties) {
    return BackOffPolicyBuilder.newBuilder()
        .delay(batchProperties.getBackoffInitialDelay().toMillis())
        .multiplier(batchProperties.getBackoffMultiplier())
        .build();
  }

  @ConditionalOnMissingBean
  @Bean
  RetryPolicy retryPolicy(final BatchProperties batchProperties) {
    CompositeRetryPolicy retryPolicy = new CompositeRetryPolicy();
    retryPolicy.setPolicies(
        ArrayUtils.toArray(
            this.noRetryPolicy(batchProperties), this.daoRetryPolicy(batchProperties)));
    return retryPolicy;
  }

  private RetryPolicy noRetryPolicy(final BatchProperties batchProperties) {
    Map<Class<? extends Throwable>, Boolean> exceptionClassifiers =
        this.skippedExceptions().stream().collect(Collectors.toMap(ex -> ex, ex -> Boolean.FALSE));
    return new SimpleRetryPolicy(batchProperties.getMaxRetries(), exceptionClassifiers, false);
  }

  private RetryPolicy daoRetryPolicy(final BatchProperties batchProperties) {
    return new SimpleRetryPolicy(
        batchProperties.getMaxRetries(),
        Map.of(
            TransientDataAccessException.class,
            true,
            RecoverableDataAccessException.class,
            true,
            NonTransientDataAccessException.class,
            false,
            EmptyResultDataAccessException.class,
            false),
        false);
  }

  // TODO: May need to Implement retry policy for Kafka,
  // need to check if required as Kafka client internally retries,
  // So may not be required to retry explicitly
  // If required create new retry policy (similar to above method) for accrual accounting job as
  // configure in respective step

  // If want to skip for all kind of exceptions, return new AlwaysSkipItemSkipPolicy
  // Skipped exceptions must also be specified in noRollback in Step configuration
  @ConditionalOnMissingBean
  @Bean
  SkipPolicy skipPolicy(final BatchProperties batchProperties) {
    Map<Class<? extends Throwable>, Boolean> exceptionClassifiers =
        this.skippedExceptions().stream().collect(Collectors.toMap(ex -> ex, ex -> Boolean.TRUE));
    return new LimitCheckingItemSkipPolicy(batchProperties.getSkipLimit(), exceptionClassifiers);
  }

  @ConditionalOnMissingBean
  @Bean
  StepExecutionListener stepExecutionListener() {
    return new LoggingStepListener();
  }

  @ConditionalOnMissingBean
  @Bean
  JobExecutionListener jobExecutionListener() {
    return new LoggingJobListener();
  }

  @ConditionalOnMissingBean
  @Bean
  List<Class<? extends Throwable>> skippedExceptions() {
    return List.of(ConstraintViolationException.class, SkipRecordException.class);
  }
}
