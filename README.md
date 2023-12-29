# Spring Batch Job implementation as Spring Cloud Task

[**Spring Batch**](https://docs.spring.io/spring-batch/reference/index.html) is a battle tested Java framework that makes it easy to write batch applications. 
Batch applications involve reliably and efficiently processing large volumes of data to and
from various data sources (files, databases, messaging middleware, and so on). 
Spring Batch is great at doing this and provides the necessary foundation to meet the stringent requirements of batch applications.
It provides mechanisms for common tasks such as **task orchestration**, **partitioning**, and **restart**.

[**Spring Cloud Task**](https://docs.spring.io/spring-cloud-task/docs/current/reference/html/) is a framework for creating and orchestrating short-lived microservices.
So It's a good fit for Spring Batch Jobs as the JVM persists until the job is completed and subsequently exits, freeing up resources.

## Introduction
This project is a simple example of how to implement a Spring Batch Job as a Spring Cloud Task.
It implements a hypothetical use case to generate Credit card statements 
containing aggregate daily transaction amounts date-wise for a particular month.
* Reads Credit card accounts from a MongoDB collection `accounts` in database `account_db` and partition on these account numbers for high performance.
* Reads transactions from MongoDB collection `transactions` in database `transaction_db` using pagination. Aggregates transaction amounts per day.
* Processes the date-wise transaction amount and writes the output to MongoDB collection `statements` in database `statement_db`.
* It is fault-tolerant i.e. try to recover from transient failures and skip bad records. 
* It supports restartability from last failure point.

## Installation
Clone this repository, import in your favourite IDE as either Maven or Gradle project.
Requires Java 21, Spring boot 3.2.0+ and Spring batch 5.1.0+.

### Docker compose
Application is bundled with [**`Spring boot Docker compose`**](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.docker-compose).
* If you have docker installed, then simply run the application in `docker` profile by passing `spring.profiles.active=docker`
  as program argument from your IDE.
* Depending on your current working directory in IDE, you may need to change `spring.docker.compose.file=spring-boot-mongodb-auditing/compose.yml`
  to `spring.docker.compose.file=compose.yml` in [**`application-docker.yml`**](src/main/resources/config/application-docker.yml)
* Make sure the host ports mapped in [**`Docker compose file`**](compose.yml) are available or change the ports and
  do the respective changes in database configurations [**`application-docker.yml`**](src/main/resources/config/application-docker.yml)

### Explicit MongoDB and Postgres installation
Change to your MongoDB URI in [**`application.yml`**](src/main/resources/config/application.yml) file as follows.
```yaml
spring:
  datasource:
    url: <Your Postgres Database URL>/<Your Database name>
    username: <Your Database username>
    password: <Your Database password>
  data:
    mongodb:
      uri: <Your MongoDB URI>
```
> [!IMPORTANT]
Make sure **flyway** is enabled as Spring Batch and Spring Cloud Task needs their [`schema`](src/main/resources/db/migration/V1.1__scdf_schema.sql) to be created.
Used internally by the framework to persist and retrieve metadata about the jobs and tasks.

### Sample Data
On first run, it creates schema and populates sample data for last three months into MongoDB collections. 
For details refer to [`DataPopulator`](src/main/java/com/ksoot/batch/DataPopulator.java).
Depending on dataset size to be created the application may take a while to start, the first time. In subsequent runs, it will start quickly.
You can change the number of accounts to be created as follows
```java
// Total number of Credit card accounts to be created
// For each account upto 10 transactions are created for each day of last 3 months
private static final int ACCOUNTS_COUNT = 1000;
// Number of records to be created in a batch
private static final int BATCH_SIZE = 1000;
```

### Job Parameters
Job may take following optional parameters, defaults are taken if not specified. Refer to [`StatementJobApplication`](src/main/java/com/techsharezone/batch/statement/StatementJobApplication.java) for more details.
* `cardNumbers` - Comma separated list of Credit card numbers to process. If not specified, all accounts are processed.
Example: `cardNumbers=5038-1972-4899-4180,5752-0862-5835-3760`
* `month` - Month for which statement is to be generated. If not specified, last month is taken.
Example: `month=2023-11`
* `forceRestart` - If set to true, job is restarted even if its last execution with same parameters was successful. 
If not specified `false` is taken as default, in this case if its last execution with same parameters was successful then Job would not execute again.
Example: `forceRestart=true`

> [!IMPORTANT]
You can pass these parameters as program arguments from your IDE as follows.
```shell
--cardNumbers=5038-1972-4899-4180,5752-0862-5835-3760 --month=2023-11 --forceRestart=true
```

## Implementation
The application uses [**`spring-batch-commons`**](https://github.com/officiallysingh/spring-batch-commons) to avail common Spring Batch components, out of box.
Maven
```xml
<dependency>
    <groupId>io.github.officiallysingh</groupId>
    <artifactId>spring-batch-commons</artifactId>
    <version>1.0</version>
</dependency>
```
Or Gradle
```groovy
implementation 'io.github.officiallysingh:spring-batch-commons:1.0'
```

### Job Configuration
Defines a Partitioned Job with a single step as follows. 
For details, refer to [`StatementJobConfiguration`](src/main/java/com/techsharezone/batch/statement/config/StatementJobConfiguration.java).
Reader and Writer are self-explanatory. Processor should contain all business logic and Multiple processors can be chained together using 
[`CompositeItemProcessor`](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/support/CompositeItemProcessor.html).
[`BeanValidatingItemProcessor`](https://docs.spring.io/spring-batch/docs/current/api/org/springframework/batch/item/validation/BeanValidatingItemProcessor.html) is used to validate the input data.
```java
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
                MongoIdGenerator.compositeIdGenerator(item.cardNumber(), item.transactionDate()))
        .build();
  }
}
```

### Job Partitioning
If specific `cardNumbers` are passed as job parameters, then the job is partitioned on these account numbers only. 
Otherwise, all accounts are processed in parallel by partitioning on account numbers.
For details refer to [`AccountsPartitioner`](https://github.com/officiallysingh/spring-boot-batch-cloud-task/blob/main/src/main/java/com/ksoot/batch/job/AccountsPartitioner.java).
```java
@Slf4j
public class AccountsPartitioner extends AbstractPartitioner {

  private final MongoTemplate accountMongoTemplate;

  private final List<String> cardNumbers;

  AccountsPartitioner(
      @Qualifier("accountMongoTemplate") final MongoTemplate accountMongoTemplate,
      final BatchProperties batchProperties,
      final List<String> cardNumbers) {
    super(batchProperties, AppConstants.CARD_NUMBERS_KEY);
    this.accountMongoTemplate = accountMongoTemplate;
    this.cardNumbers = cardNumbers;
  }

  @Override
  public List<String> partitioningList() {
    final Bson condition =
        CollectionUtils.isNotEmpty(this.cardNumbers)
            ? in("card_number", this.cardNumbers)
            : Filters.empty();
    return this.accountMongoTemplate
        .getCollection("accounts")
        .find(condition)
        .projection(fields(excludeId(), include("card_number")))
        .sort(ascending("card_number"))
        .map(doc -> doc.getString("card_number"))
        .into(new ArrayList<>());
  }
}
```

## Author
[**Rajveer Singh**](https://www.linkedin.com/in/rajveer-singh-589b3950/), In case you find any issues or need any support, please email me at raj14.1984@gmail.com

## References
* Refer to Spring batch common components and utilities [**`spring-batch-commons`**](https://github.com/officiallysingh/spring-batch-commons).
* For exception handling refer to [**`spring-boot-problem-handler`**](https://github.com/officiallysingh/spring-boot-problem-handler).
* For more details on Spring Batch refer to [**`Spring Batch Reference`**](https://docs.spring.io/spring-batch/reference/index.html).
* To deploy on Spring cloud Data Flow refer to [**`Spring Cloud Data Flow Reference`**](https://spring.io/projects/spring-cloud-dataflow/).