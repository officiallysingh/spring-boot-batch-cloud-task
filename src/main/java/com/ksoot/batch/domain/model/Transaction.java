//package com.ksoot.batch.domain.model;
//
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.OffsetDateTime;
//import java.util.Objects;
//
//import lombok.*;
//import lombok.experimental.Accessors;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.annotation.PersistenceCreator;
//import org.springframework.data.mongodb.core.index.CompoundIndex;
//import org.springframework.data.mongodb.core.index.Indexed;
//import org.springframework.data.mongodb.core.mapping.Document;
//import org.springframework.data.mongodb.core.mapping.Field;
//import org.springframework.data.mongodb.core.mapping.FieldType;
//
//@Getter
//@Accessors(chain = true, fluent = true)
//@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceCreator))
//@Document(collection = "transactions")
//@CompoundIndex(
//        name = "idxTransactionsCardNumberTxnDatetime",
//        def = "{'card_Number' : 1, 'datetime': 1}")
//@Valid
////@Getter
//@ToString
//@EqualsAndHashCode(of = "id")
//public class Transaction {
//
//  @Id
//  private String id;
//
//  @NotEmpty
//  @Size(max = 50)
//  @Indexed(name = "idxTransactionsCardNumber", background = true)
//  @Field(name = "card_Number")
//  private String cardNumber;
//
//  @PastOrPresent
//  @Indexed(name = "idxTransactionsDatetime", background = true)
//  @Field(name = "datetime")
//  private OffsetDateTime datetime;
//
//  @NotNull
//  @PositiveOrZero
//  @Field(name = "amount", targetType = FieldType.DECIMAL128)
//  private BigDecimal amount;
//
//  public Transaction(
//      final String cardNumber,
//      final OffsetDateTime datetime,
//      final BigDecimal amount) {
//    this.cardNumber = cardNumber;
//    this.datetime = datetime;
//    this.amount = amount;
//  }
//}
