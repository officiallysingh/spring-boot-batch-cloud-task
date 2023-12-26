package com.ksoot.batch.job.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractJobExecutor {

  @Autowired protected JobLauncher jobLauncher;

  @Autowired protected JobExplorer jobExplorer;

  protected final void execute(
      final Job job, final JobParametersBuilder jobParametersBuilder, final boolean forceRestart)
      throws JobInstanceAlreadyCompleteException,
          JobExecutionAlreadyRunningException,
          JobParametersInvalidException,
          JobRestartException {
    JobParameters jobParameters = jobParametersBuilder.toJobParameters();
    log.info(
        "Submitting Job: "
            + job.getName()
            + (forceRestart ? " forcefully" : "")
            + " for execution with parameters -->"
            + jobParameters);

    try {
      this.jobLauncher.run(job, jobParameters);
    } catch (final JobInstanceAlreadyCompleteException e) {
      // Should not forceRestart if job is already running
      // | JobExecutionAlreadyRunningException
      if (forceRestart) {
        jobParameters = jobParametersBuilder.getNextJobParameters(job).toJobParameters();
        this.jobLauncher.run(job, jobParameters);
      } else {
        throw e;
      }
    }
  }
}
