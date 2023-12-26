package com.ksoot.batch.job.common;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

@Slf4j
public class LoggingJobListener implements JobExecutionListener {

  @Override
  public void beforeJob(final JobExecution jobExecution) {
    jobExecution.setStartTime(LocalDateTime.now());
    log.info(jobExecution.getJobInstance().getJobName() + " Job is beginning execution");
  }

  @Override
  public void afterJob(final JobExecution jobExecution) {
    jobExecution.setEndTime(LocalDateTime.now());
    log.info(
        jobExecution.getJobInstance().getJobName()
            + " Job has completed in time: "
            + Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime())
            + " with the status "
            + jobExecution.getStatus());
  }
}
