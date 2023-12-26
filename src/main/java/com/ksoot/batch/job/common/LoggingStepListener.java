package com.ksoot.batch.job.common;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class LoggingStepListener implements StepExecutionListener {

  @Override
  public void beforeStep(StepExecution stepExecution) {
    stepExecution.setStartTime(LocalDateTime.now());
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    stepExecution.setEndTime(LocalDateTime.now());

    log.info(
        stepExecution.getStepName()
            + " Step completed in time: "
            + Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime())
            + "\nSummary"
            + stepExecution.getSummary());

    if (stepExecution.getProcessSkipCount() > 0) {
      return StepStatus.COMPLETED_WITH_SKIPS;
    } else {
      return stepExecution.getExitStatus();
    }
  }
}
