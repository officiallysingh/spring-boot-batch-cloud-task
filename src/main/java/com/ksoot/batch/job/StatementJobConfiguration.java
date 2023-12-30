package com.ksoot.batch.job;

import static com.ksoot.spring.batch.common.AbstractPartitioner.PARTITION_DATA_VALUE_SEPARATOR;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import com.ksoot.batch.domain.AppConstants;
import com.ksoot.batch.domain.model.DailyTransaction;
import com.ksoot.batch.domain.model.Statement;
import com.ksoot.batch.utils.DateTimeUtils;
import com.ksoot.spring.batch.common.BatchConfiguration;
import com.ksoot.spring.batch.common.JobConfigurationSupport;
import com.ksoot.spring.batch.common.MongoAggregationPagingItemReader;
import com.ksoot.spring.batch.common.MongoIdGenerator;
import com.ksoot.spring.batch.common.MongoItemWriters;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.validator.BeanValidatingItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
@AutoConfigureAfter(value = {BatchConfiguration.class})
class StatementJobConfiguration extends JobConfigurationSupport<DailyTransaction, Statement> {

  @Bean
  Job statementJob(
      @Qualifier("statementJobPartitioner") final AccountsPartitioner statementJobPartitioner,
      final ItemReader<DailyTransaction> transactionReader,
      final ItemProcessor<DailyTransaction, Statement> statementProcessor,
      final ItemWriter<Statement> statementWriter)
      throws Exception {
    return newPartitionedJob(
        AppConstants.STATEMENT_JOB_NAME,
        statementJobPartitioner,
        transactionReader,
        statementProcessor,
        statementWriter);
  }

  @Bean
  @StepScope
  AccountsPartitioner statementJobPartitioner(
      @Qualifier("accountMongoTemplate") final MongoTemplate accountMongoTemplate,
      @Value("#{jobParameters['" + AppConstants.JOB_PARAM_NAME_CARD_NUMBERS + "']}")
          final List<String> cardNumbers) {
    return new AccountsPartitioner(accountMongoTemplate, this.batchProperties, cardNumbers);
  }

  @Bean
  @StepScope
  MongoAggregationPagingItemReader<DailyTransaction> transactionReader(
      @Qualifier("transactionMongoTemplate") final MongoTemplate transactionMongoTemplate,
      @Value("#{jobParameters['" + AppConstants.JOB_PARAM_NAME_STATEMENT_MONTH + "']}")
          final String month,
      @Value("#{stepExecutionContext['" + AppConstants.CARD_NUMBERS_KEY + "']}")
          final String cardNumbers) {

    final YearMonth statementMonth = YearMonth.parse(month);
    List<String> cardNumbersList =
        StringUtils.isNotBlank(cardNumbers)
            ? Arrays.asList(cardNumbers.split(PARTITION_DATA_VALUE_SEPARATOR))
            : Collections.emptyList();

    OffsetDateTime fromDateTime =
        statementMonth.atDay(1).atStartOfDay().atOffset(DateTimeUtils.ZONE_OFFSET_IST);
    OffsetDateTime tillDateTime =
        statementMonth
            .atEndOfMonth()
            .plusDays(1)
            .atStartOfDay()
            .atOffset(DateTimeUtils.ZONE_OFFSET_IST);
    Criteria condition = null;
    if (CollectionUtils.isNotEmpty(cardNumbersList)) {
      condition =
          Criteria.where("card_number")
              .in(cardNumbersList)
              .and("datetime")
              .gte(fromDateTime)
              .lt(tillDateTime);
    } else {
      condition = Criteria.where("datetime").gte(fromDateTime).lt(tillDateTime);
      //      condition = new Criteria();
    }

    final AggregationOperation[] aggregationOperations =
        new AggregationOperation[] {
          match(condition),
          project("card_number", "amount", "datetime")
              .andExpression("{$toDate: '$datetime'}")
              .as("date"),
          group("card_number", "date").sum("amount").as("amount"),
          project("card_number", "date", "amount").andExclude("_id"),
          sort(Sort.Direction.ASC, "card_number", "date")
        };

    MongoAggregationPagingItemReader<DailyTransaction> itemReader =
        new MongoAggregationPagingItemReader<>();
    itemReader.setName("transactionsReader");
    itemReader.setTemplate(transactionMongoTemplate);
    itemReader.setCollection("transactions");
    itemReader.setTargetType(DailyTransaction.class);
    itemReader.setAggregationOperation(aggregationOperations);
    itemReader.setPageSize(this.batchProperties.getPageSize());
    return itemReader;
  }

  @Bean
  CompositeItemProcessor<DailyTransaction, Statement> statementProcessor(
      final BeanValidatingItemProcessor<DailyTransaction> beanValidatingDailyTransactionProcessor) {
    final CompositeItemProcessor<DailyTransaction, Statement> compositeProcessor =
        new CompositeItemProcessor<>();
    compositeProcessor.setDelegates(
        Arrays.asList(beanValidatingDailyTransactionProcessor, new StatementProcessor()));
    return compositeProcessor;
  }

  @Bean
  BeanValidatingItemProcessor<DailyTransaction> beanValidatingDailyTransactionProcessor(
      final LocalValidatorFactoryBean validatorFactory) {
    return new BeanValidatingItemProcessor<>(validatorFactory);
  }

  // Idempotent upsert
  @Bean
  MongoItemWriter<Statement> statementWriter(
      @Qualifier("mongoTemplate") final MongoTemplate statementMongoTemplate) {
    return MongoItemWriters.<Statement>template(statementMongoTemplate)
        .collection("statements")
        .idGenerator(
            (Statement item) ->
                MongoIdGenerator.compositeIdGenerator(item.getCardNumber(), item.getTransactionDate()))
        .build();
  }
}
