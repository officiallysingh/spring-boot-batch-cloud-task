package com.ksoot.batch;

import com.ksoot.batch.job.StatementJobExecutor;
import com.ksoot.batch.utils.DateTimeUtils;
import jakarta.annotation.PostConstruct;
import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

@EnableTask
@SpringBootApplication
@Slf4j
public class StatementJobTask {

  public static void main(final String[] args) {
    SpringApplication.run(StatementJobTask.class, args);
  }

  @Bean
  public ApplicationRunner applicationRunner(
      final StatementJobExecutor statementJobExecutor, final DataPopulator dummyDataPopulator) {
    return new StatementJobRunner(statementJobExecutor, dummyDataPopulator);
  }

  @PostConstruct
  public void init() {
    log.info("System time zone: " + DateTimeUtils.SYSTEM_ZONE_DISPLAY_NAME);
  }

  @Slf4j
  public static class StatementJobRunner implements ApplicationRunner {

    private final StatementJobExecutor statementJobExecutor;

    private final DataPopulator dataPopulator;

    @Value("${month:#{T(com.ksoot.batch.utils.DateTimeUtils).previousMonthIST()}}")
    private YearMonth month;

    @Value("${cardNumbers:#{null}}")
    private List<String> cardNumbers;

    @Value("${forceRestart:false}")
    private boolean forceRestart;

    public StatementJobRunner(
        final StatementJobExecutor statementJobExecutor, final DataPopulator dataPopulator) {
      this.statementJobExecutor = statementJobExecutor;
      this.dataPopulator = dataPopulator;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {

      if(!this.month.isBefore(DateTimeUtils.currentMonthIST())) {
        throw new IllegalArgumentException("Only past Months allowed");
      }

      this.dataPopulator.createSchema();
      this.dataPopulator.createData();

      log.info(
          "Starting Statement job task with parameters >> month: "
              + this.month
              + ", cardNumbers: "
              + (CollectionUtils.isNotEmpty(this.cardNumbers) ? String.join(",", this.cardNumbers) : "All")
              + ", forceRestart: "
              + this.forceRestart);
      this.statementJobExecutor.executeStatementJob(
          this.month, this.forceRestart, this.cardNumbers);
    }
  }
}
