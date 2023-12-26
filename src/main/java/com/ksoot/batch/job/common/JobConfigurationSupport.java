package com.ksoot.batch.job.common;

import com.ksoot.batch.config.MongoPropertiesConfig;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@AutoConfigureAfter(value = {BatchConfiguration.class, MongoPropertiesConfig.class})
public abstract class JobConfigurationSupport<R, W> {

  @Autowired protected BatchProperties batchProperties;
  @Autowired private JobRepository jobRepository;

  //    @Autowired
  //    private TaskExecutor taskExecutor;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JobExecutionListener jobExecutionListener;
  @Autowired private StepExecutionListener stepExecutionListener;
  @Autowired private BackOffPolicy backOffPolicy;
  @Autowired private RetryPolicy retryPolicy;
  @Autowired private SkipPolicy skipPolicy;
  @Autowired private ObjectProvider<SkipListener<R, W>> skipListenerProvider;
  @Autowired private List<Class<? extends Throwable>> skippedExceptions;
  @Autowired private JobParametersIncrementer jobParametersIncrementer;

  protected Job newPartitionedJob(
      final String jobName,
      final Partitioner partitioner,
      final ItemReader<R> reader,
      final ItemProcessor<R, W> processor,
      final ItemWriter<W> writer)
      throws Exception {
    return new JobBuilder(jobName, this.jobRepository)
        .incrementer(this.jobParametersIncrementer())
        .listener(this.jobExecutionListener())
        .start(this.managerStep(partitioner, newStep(jobName, reader, processor, writer)))
        .build();
  }

  protected Step managerStep(final Partitioner partitioner, final Step step) throws Exception {
    return new StepBuilder(step.getName() + "-Manager", this.jobRepository)
        .partitioner(step.getName(), partitioner)
        .partitionHandler(this.partitionHandler(step))
        .build();
  }

  protected PartitionHandler partitionHandler(final Step step) throws Exception {
    // Initialized with default SyncTaskExecutor,
    // Async task executor does not work, throw Job scope not available exception
    TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
    partitionHandler.setStep(step);
    partitionHandler.setGridSize(this.batchProperties.getPartitionSize());
    partitionHandler.afterPropertiesSet();
    return partitionHandler;
  }

  protected Step newStep(
      final String name,
      final ItemReader<R> reader,
      final ItemProcessor processor,
      final ItemWriter<W> writer) {
    FaultTolerantStepBuilder stepBuilder =
        new StepBuilder(name + "-Step", this.jobRepository)
            .<R, W>chunk(this.batchProperties.getChunkSize(), this.transactionManager)
            .allowStartIfComplete(true)
            .listener(this.stepExecutionListener())
            //                        .taskExecutor(this.taskExecutor) // Multithreading in Step
            // need verification if it works
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipPolicy(this.skipPolicy())
            .listener(this.skipListener())
            .retryPolicy(this.retryPolicy())
            .backOffPolicy(this.backOffPolicy())
            .noRollback(EmptyResultDataAccessException.class);
    this.skippedExceptions.stream().forEach(stepBuilder::noRollback);
    return stepBuilder.build();
  }

  protected JobParametersIncrementer jobParametersIncrementer() {
    return this.jobParametersIncrementer;
  }

  protected BackOffPolicy backOffPolicy() {
    return this.backOffPolicy;
  }

  protected RetryPolicy retryPolicy() {
    return this.retryPolicy;
  }

  protected SkipPolicy skipPolicy() {
    return this.skipPolicy;
  }

  protected JobExecutionListener jobExecutionListener() {
    return this.jobExecutionListener;
  }

  protected StepExecutionListener stepExecutionListener() {
    return this.stepExecutionListener;
  }

  protected SkipListener<R, W> skipListener() {
    return this.skipListenerProvider.getIfAvailable(
        () ->
            new SkipListener<>() {
              @Override
              public void onSkipInProcess(final R item, final Throwable exception) {
                log.warn(
                    "Skipped item while processing: " + item + " due to skippable exception",
                    exception);
              }
            });
  }
}
