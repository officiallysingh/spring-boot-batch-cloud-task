package com.ksoot.batch;

import com.ksoot.batch.job.StatementJobExecutor;
import com.ksoot.batch.utils.DateTimeUtils;
import jakarta.annotation.PostConstruct;

import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;

@EnableTask
@EnableBatchProcessing
@SpringBootApplication
@Slf4j
public class StatementJobApplication {

  public static void main(final String[] args) {
    SpringApplication.run(StatementJobApplication.class, args);
  }

  @Bean
  public ApplicationRunner applicationRunner(final StatementJobExecutor statementJobExecutor,
                                             final DummyDataPopulator dummyDataPopulator) {
    return new StatementJobRunner(statementJobExecutor, dummyDataPopulator);
  }

  @PostConstruct
  public void init() {
    log.info("System time zone: " + DateTimeUtils.SYSTEM_ZONE_DISPLAY_NAME);
  }

  @Slf4j
  public static class StatementJobRunner implements ApplicationRunner {

    @Value("${date:#{T(java.time.YearMonth).now().minusMonths(1)}}")
    private YearMonth month;

    @Value("${forceRestart:false}")
    private boolean forceRestart;

    @Value("${cardNumbers:#{null}}")
    private List<String> cardNumbers;

    private final StatementJobExecutor statementJobExecutor;

    private final DummyDataPopulator dummyDataPopulator;

    public StatementJobRunner(final StatementJobExecutor statementJobExecutor,
                              final DummyDataPopulator dummyDataPopulator) {
      this.statementJobExecutor = statementJobExecutor;
      this.dummyDataPopulator = dummyDataPopulator;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
//      this.dummyDataPopulator.createData();
      log.info(
              "Starting Statement job task with parameters >> month: "
                      + this.month
                      + ", cardNumbers: "
                      + this.cardNumbers
                      + ", forceRestart: "
                      + this.forceRestart);
      this.statementJobExecutor.executeStatementJob(this.month, this.forceRestart, this.cardNumbers);
    }
  }

//  @Bean
//  public Converter<YearMonth, String> yearMonthToStringConverter() {
//    return new YearMonthToStringConverter();
//  }
//
//  class YearMonthToStringConverter implements Converter<YearMonth, String> {
//
//    @Override
//    public String convert(final YearMonth yearMonth) {
//      return yearMonth.toString(); // Or use a specific format if needed
//    }
//  }
}