package com.ksoot.batch.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceCreator))
@Document(collection = "statements")
@CompoundIndex(
    unique = true,
    name = "idx_unq_card_number_transaction_date",
    def = "{'card_number' : 1, 'transaction_date': 1}")
@Valid
@ToString
@EqualsAndHashCode(of = "id")
public class Statement {

  @Id private String id;

  @NotEmpty
  @Size(max = 50)
  @Indexed(name = "idx_card_number", background = true)
  @Field(name = "card_number")
  private String cardNumber;

  @NotNull
  @PastOrPresent
  @Indexed(name = "idx_transaction_date", background = true)
  @Field(name = "transaction_date")
  private LocalDate transactionDate;

  @NotNull
  @PositiveOrZero
  @Field(name = "daily_amount", targetType = FieldType.DECIMAL128)
  private BigDecimal dailyAmount;

  @PastOrPresent
  @Field(name = "created_on")
  private OffsetDateTime createdOn;

  public static Statement of(
      final String cardNumber, final LocalDate transactionDate, final BigDecimal dailyAmount) {
    return new Statement(null, cardNumber, transactionDate, dailyAmount, OffsetDateTime.now());
  }
}
