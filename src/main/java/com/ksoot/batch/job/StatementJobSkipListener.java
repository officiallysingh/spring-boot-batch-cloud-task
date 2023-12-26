package com.ksoot.batch.job;

import com.ksoot.batch.domain.model.DailyTransaction;
import com.ksoot.batch.domain.model.Statement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class StatementJobSkipListener implements SkipListener<DailyTransaction, Statement> {

  //    private StepExecution stepExecution;

  //    @BeforeStep
  //    public void saveStepExecution(final StepExecution stepExecution) {
  //        this.stepExecution = stepExecution;
  //    }

  //    @OnSkipInProcess
  // TODO: Implement logic to handle skipped records
  // May save such records with skip reason in DB and retry later
  @Override
  public void onSkipInProcess(final DailyTransaction item, final Throwable exception) {
    log.warn("Skipped item while processing: " + item + " due to skippable exception", exception);
  }
}
