package com.ksoot.batch.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Accessors(chain = true, fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceCreator))
@Valid
@ToString
@EqualsAndHashCode(of = {"cardNumber", "date"})
public class DailyTransaction {

  @NotEmpty
  @Field(name = "card_number")
  private String cardNumber;

  @NotNull
  @Field(name = "date")
  private LocalDate date;

  @PositiveOrZero
  @Field(name = "amount")
  private BigDecimal amount;

  DailyTransaction(final String cardNumber, final String date, final BigDecimal amount) {
    this.cardNumber = cardNumber;
    this.date = LocalDate.parse(date);
    this.amount = amount;
  }
}
