// package com.ksoot.batch.domain.model;
//
// import jakarta.validation.Valid;
// import jakarta.validation.constraints.*;
// import lombok.AccessLevel;
// import lombok.AllArgsConstructor;
// import lombok.EqualsAndHashCode;
// import lombok.Getter;
// import lombok.experimental.Accessors;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.annotation.PersistenceCreator;
// import org.springframework.data.mongodb.core.index.CompoundIndex;
// import org.springframework.data.mongodb.core.index.Indexed;
// import org.springframework.data.mongodb.core.mapping.Document;
// import org.springframework.data.mongodb.core.mapping.Field;
// import org.springframework.data.mongodb.core.mapping.FieldType;
//
// import java.math.BigDecimal;
// import java.time.OffsetDateTime;
//
// @Getter
// @Accessors(chain = true, fluent = true)
// @AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceCreator))
// @Document(collection = "accounts")
// @Valid
// @EqualsAndHashCode(of = "id")
// public class Account {
//
//    @Id
//    private String id;
//
//    @NotEmpty
//    @Size(max = 50)
//    @Indexed(name = "idx_card_number", background = true, unique = true)
//    @Field(name = "card_number")
//    private String cardNumber;
//
//    @NotEmpty
//    @Size(max = 50)
//    @Indexed(name = "idx_customer_name", background = true)
//    @Field(name = "customer_name")
//    private String customerName;
//
//    public Account(
//            final String cardNumber,
//            final String customerName) {
//        this.cardNumber = cardNumber;
//        this.customerName = customerName;
//    }
// }
