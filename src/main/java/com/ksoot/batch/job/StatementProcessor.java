package com.ksoot.batch.job;

import com.ksoot.batch.domain.model.DailyTransaction;
import com.ksoot.batch.domain.model.Statement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class StatementProcessor implements ItemProcessor<DailyTransaction, Statement> {

  @Override
  public Statement process(final DailyTransaction item) {
    return Statement.of(item.cardNumber(), item.date(), item.amount());
  }
}
