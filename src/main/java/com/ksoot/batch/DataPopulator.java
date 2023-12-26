package com.ksoot.batch;

import static com.ksoot.batch.utils.DateTimeUtils.ZONE_ID_IST;
import static com.ksoot.batch.utils.DateTimeUtils.ZONE_OFFSET_IST;

import com.mongodb.client.MongoCollection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DependsOn({"mongoDBConfig"})
class DataPopulator {

  private static final Faker faker =
      new Faker(new Locale.Builder().setLanguage("en").setRegion("US").build());
  private static final Random random = new Random();
  private static final int ACCOUNTS_COUNT = 1000;
  private static final int BATCH_SIZE = 1000;
  private static final BigDecimal MIN_VALUE = BigDecimal.valueOf(100);
  private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(1000);
  private static final int SCALE = 2;
  private final MongoTemplate statementMongoTemplate;
  private final MongoTemplate accountMongoTemplate;
  private final MongoTemplate transactionMongoTemplate;

  DataPopulator(
      @Qualifier("mongoTemplate") final MongoTemplate statementMongoTemplate,
      @Qualifier("accountMongoTemplate") final MongoTemplate accountMongoTemplate,
      @Qualifier("transactionMongoTemplate") final MongoTemplate transactionMongoTemplate) {
    this.statementMongoTemplate = statementMongoTemplate;
    this.accountMongoTemplate = accountMongoTemplate;
    this.transactionMongoTemplate = transactionMongoTemplate;
  }

  private static OffsetDateTime generateOffsetDateTime(final LocalDate date) {
    Date randomDate = faker.date().birthday();
    LocalTime randomLocalTime = randomDate.toInstant().atZone(ZONE_ID_IST).toLocalTime();
    return OffsetDateTime.of(date, randomLocalTime, ZONE_OFFSET_IST);
  }

  private static BigDecimal generateTransactionAmount() {
    BigDecimal range = MAX_VALUE.subtract(MIN_VALUE);
    BigDecimal randomValue = MIN_VALUE.add(range.multiply(BigDecimal.valueOf(random.nextDouble())));
    return randomValue.setScale(SCALE, RoundingMode.HALF_UP);
  }

  private void createSchema() {
    log.info("Creating Schema");
    if (!this.statementMongoTemplate.collectionExists("statements")) {
      this.statementMongoTemplate.createCollection("statements");
      log.info("Created Collection: statements");
      Index indexCardNumber =
          new Index().named("idx_card_number").on("card_number", Sort.Direction.ASC);
      this.statementMongoTemplate.indexOps("statements").ensureIndex(indexCardNumber);
      Index indexTransactionDate =
          new Index().named("idx_transaction_date").on("transaction_date", Sort.Direction.ASC);
      this.statementMongoTemplate.indexOps("statements").ensureIndex(indexTransactionDate);
      Index indexUnqCardNumberTransactionDate =
          new Index()
              .named("idx_unq_card_number_transaction_date")
              .on("card_number", Sort.Direction.ASC)
              .on("transaction_date", Sort.Direction.ASC)
              .unique();
      this.statementMongoTemplate
          .indexOps("statements")
          .ensureIndex(indexUnqCardNumberTransactionDate);
    } else {
      log.info("Collection already exists: statements");
    }

    if (!this.accountMongoTemplate.collectionExists("accounts")) {
      this.accountMongoTemplate.createCollection("accounts");
      log.info("Created Collection: accounts");
      Index indexCardNumber =
          new Index().named("idx_card_number").on("card_number", Sort.Direction.ASC).unique();
      this.accountMongoTemplate.indexOps("accounts").ensureIndex(indexCardNumber);
      Index indexCustomerName =
          new Index().named("idx_customer_name").on("customer_name", Sort.Direction.ASC);
      this.accountMongoTemplate.indexOps("accounts").ensureIndex(indexCustomerName);
    } else {
      log.info("Collection already exists: accounts");
    }

    if (!this.transactionMongoTemplate.collectionExists("transactions")) {
      this.transactionMongoTemplate.createCollection("transactions");
      log.info("Created Collection: transactions");
      Index indexCardNumber =
          new Index().named("idx_card_number").on("card_number", Sort.Direction.ASC);
      this.transactionMongoTemplate.indexOps("transactions").ensureIndex(indexCardNumber);

      Index indexDatetime = new Index().named("idx_datetime").on("datetime", Sort.Direction.ASC);
      this.transactionMongoTemplate.indexOps("transactions").ensureIndex(indexDatetime);
    } else {
      log.info("Collection already exists: transactions");
    }
  }

  public void createData() {
    this.createSchema();
    log.info("Creating sample data");
    final MongoCollection<Document> accountsCollection =
        this.accountMongoTemplate.getCollection("accounts");
    if (accountsCollection.countDocuments() > 0) {
      log.info("Dummy data already exists");
      return;
    }
    final MongoCollection<Document> transactionsCollection =
        this.transactionMongoTemplate.getCollection("transactions");

    final List<Document> accounts = new ArrayList<>(BATCH_SIZE);
    final List<Document> transactions = new ArrayList<>(10 * BATCH_SIZE);

    final YearMonth currentMonth = YearMonth.now(ZONE_ID_IST);
    final List<YearMonth> months =
        List.of(
            currentMonth.minusMonths(3), currentMonth.minusMonths(2), currentMonth.minusMonths(1));

    int accountsCount = 0;
    int transactionsCount = 0;
    for (int i = 1; i <= ACCOUNTS_COUNT; i++) {
      String cardName = faker.business().creditCardNumber();
      final Document account = new Document("_id", new ObjectId());
      account.append("card_number", cardName).append("customer_name", faker.name().fullName());
      accounts.add(account);

      for (final YearMonth month : months) {
        for (LocalDate date = month.atDay(1);
            !date.isAfter(month.atEndOfMonth());
            date = date.plusDays(1)) {
          int perDayTransactions = random.nextInt(11);
          for (int j = 0; j < perDayTransactions; j++) {
            final Document transaction = new Document("_id", new ObjectId());
            transaction
                .append("card_number", cardName)
                .append("datetime", generateOffsetDateTime(date))
                .append("amount", generateTransactionAmount());
            transactions.add(transaction);

            transactionsCount++;
            if (transactionsCount % BATCH_SIZE == 0) {
              transactionsCollection.insertMany(transactions);
              transactions.clear();
              log.info(
                  "Created " + transactionsCount + " Credit card transactions, processed till date: " + date);
            }
          }
        }
      }

      accountsCount++;
      if (accountsCount % BATCH_SIZE == 0) {
        accountsCollection.insertMany(accounts);
        accounts.clear();
        log.info("Created " + accountsCount + " Credit card accounts");
      }
    }

    if (CollectionUtils.isNotEmpty(accounts)) {
      accountsCollection.insertMany(accounts);
      accounts.clear();
    }
    if (CollectionUtils.isNotEmpty(transactions)) {
      transactionsCollection.insertMany(transactions);
      transactions.clear();
    }
    log.info("Created " + accountsCount + " Credit card accounts");
    log.info("Created " + transactionsCount + " transactions for months: " + months);
  }
}
