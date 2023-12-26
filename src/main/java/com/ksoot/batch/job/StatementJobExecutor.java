package com.ksoot.batch.job;

import com.ksoot.batch.domain.AppConstants;
import com.ksoot.spring.batch.common.AbstractJobExecutor;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StatementJobExecutor extends AbstractJobExecutor {

  private final Job statementJob;

  StatementJobExecutor(@Qualifier(AppConstants.STATEMENT_JOB_NAME) final Job statementJob) {
    this.statementJob = statementJob;
  }

  public void executeStatementJob(
      final YearMonth month, final boolean forceRestart, final List<String> cardNumbers)
      throws JobInstanceAlreadyCompleteException,
          JobExecutionAlreadyRunningException,
          JobParametersInvalidException,
          JobRestartException {

    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder(this.jobExplorer);
    jobParametersBuilder.addJobParameter(
        AppConstants.JOB_PARAM_NAME_STATEMENT_MONTH, month.toString(), String.class);
    if (CollectionUtils.isNotEmpty(cardNumbers)) {
      Collections.sort(cardNumbers);
      jobParametersBuilder.addJobParameter(
          AppConstants.JOB_PARAM_NAME_CARD_NUMBERS, cardNumbers, List.class, true);
    } else {
      jobParametersBuilder.addJobParameter(
          AppConstants.JOB_PARAM_NAME_CARD_NUMBERS, Collections.emptyList(), List.class, true);
    }

    this.execute(this.statementJob, jobParametersBuilder, forceRestart);
  }
}
