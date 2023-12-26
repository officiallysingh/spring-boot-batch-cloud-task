package com.ksoot.batch.job;

import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.ascending;

import com.ksoot.batch.domain.AppConstants;
import com.ksoot.spring.batch.common.AbstractPartitioner;
import com.ksoot.spring.batch.common.BatchProperties;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;

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
