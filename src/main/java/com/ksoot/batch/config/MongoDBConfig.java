package com.ksoot.batch.config;

import com.ksoot.batch.utils.DateTimeUtils;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.SpringDataMongoDB;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
class MongoDBConfig extends AbstractMongoClientConfiguration {

  private final MongoProperties mongoProperties;

  private final CodecRegistry customCodecRegistry;

  MongoDBConfig(final MongoProperties mongoProperties) {
    this.mongoProperties = mongoProperties;
    this.customCodecRegistry =
        CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(new OffsetDateTimeCodec(), new ZonedDateTimeCodec()),
            MongoClientSettings.getDefaultCodecRegistry());
  }

  @Override
  protected void configureClientSettings(final MongoClientSettings.Builder builder) {
    builder
        .applyConnectionString(new ConnectionString(this.mongoProperties.determineUri()))
        .uuidRepresentation(this.mongoProperties.getUuidRepresentation())
        .codecRegistry(this.customCodecRegistry);
  }

  @Override
  protected boolean autoIndexCreation() {
    return this.mongoProperties.isAutoIndexCreation();
  }

  @Override
  protected String getDatabaseName() {
    return this.mongoProperties.getDatabase();
  }

  @Override
  protected Collection<String> getMappingBasePackages() {
    String mainClassName = System.getProperty("sun.java.command");
    mainClassName =
        mainClassName.contains(" ")
            ? mainClassName.substring(0, mainClassName.indexOf(' '))
            : mainClassName;
    String defaultPackageName = mainClassName.substring(0, mainClassName.lastIndexOf('.'));
    return Collections.singleton(defaultPackageName); // Main class package name
  }

  @Bean
  MongoTemplate accountMongoTemplate(
      @Qualifier("accountMongoProperties") final MongoProperties accountMongoProperties,
      final MappingMongoConverter converter) {

    final MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(accountMongoProperties.determineUri()))
            .uuidRepresentation(this.mongoProperties.getUuidRepresentation())
            .codecRegistry(this.customCodecRegistry)
            .build();

    final MongoDatabaseFactory databaseFactory =
        new SimpleMongoClientDatabaseFactory(
            MongoClients.create(settings, SpringDataMongoDB.driverInformation()),
            accountMongoProperties.getDatabase());

    return new MongoTemplate(databaseFactory, converter);
  }

  @Bean
  MongoTemplate transactionMongoTemplate(
      @Qualifier("transactionMongoProperties") final MongoProperties transactionMongoProperties,
      final MappingMongoConverter converter) {

    final MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(transactionMongoProperties.determineUri()))
            .uuidRepresentation(this.mongoProperties.getUuidRepresentation())
            .codecRegistry(this.customCodecRegistry)
            .build();

    final MongoDatabaseFactory databaseFactory =
        new SimpleMongoClientDatabaseFactory(
            MongoClients.create(settings, SpringDataMongoDB.driverInformation()),
            transactionMongoProperties.getDatabase());

    return new MongoTemplate(databaseFactory, converter);
  }

  @Bean
  ValidatingMongoEventListener validatingMongoEventListener(
      final LocalValidatorFactoryBean factory) {
    return new ValidatingMongoEventListener(factory);
  }

  @Bean
  LocalValidatorFactoryBean validatorFactory() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    return validator;
  }

  @Bean
  MongoTransactionManager transactionManager(final MongoDatabaseFactory mongoDatabaseFactory) {
    return new MongoTransactionManager(mongoDatabaseFactory);
  }

  @Override
  public MongoCustomConversions customConversions() {
    List<Object> converters = new ArrayList<>();
    converters.add(new ZonedDateTimeReadConverter());
    converters.add(new ZonedDateTimeWriteConverter());
    converters.add(new OffsetDateTimeReadConverter());
    converters.add(new OffsetDateTimeWriteConverter());
    return new MongoCustomConversions(converters);
  }

  @ReadingConverter
  class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(final Date source) {
      return source.toInstant().atZone(DateTimeUtils.SYSTEM_ZONE_ID);
    }
  }

  @WritingConverter
  class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {

    @Override
    public Date convert(final ZonedDateTime source) {
      return Date.from(source.toInstant());
    }
  }

  @ReadingConverter
  class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(final Date source) {
      return source.toInstant().atZone(DateTimeUtils.SYSTEM_ZONE_ID).toOffsetDateTime();
    }
  }

  @WritingConverter
  class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {

    @Override
    public Date convert(final OffsetDateTime source) {
      return Date.from(source.toInstant());
    }
  }

  class OffsetDateTimeCodec implements Codec<OffsetDateTime> {

    @Override
    public void encode(
        final BsonWriter writer, final OffsetDateTime value, final EncoderContext encoderContext) {
      if (value != null) {
        writer.writeDateTime(Date.from(value.toInstant()).getTime());
      } else {
        writer.writeNull();
      }
    }

    @Override
    public OffsetDateTime decode(final BsonReader reader, final DecoderContext decoderContext) {
      BsonType type = reader.getCurrentBsonType();

      if (type == BsonType.NULL) {
        reader.readNull();
        return null;
      } else if (type == BsonType.DATE_TIME) {
        long milliseconds = reader.readDateTime();
        Instant instant = Instant.ofEpochMilli(milliseconds);
        return OffsetDateTime.ofInstant(instant, DateTimeUtils.SYSTEM_ZONE_ID);
      } else {
        throw new UnsupportedOperationException(
            "Unsupported BSON type for OffsetDateTime: " + type);
      }
    }

    @Override
    public Class<OffsetDateTime> getEncoderClass() {
      return OffsetDateTime.class;
    }
  }

  class ZonedDateTimeCodec implements Codec<ZonedDateTime> {

    @Override
    public void encode(
        final BsonWriter writer, final ZonedDateTime value, final EncoderContext encoderContext) {
      if (value != null) {
        writer.writeDateTime(Date.from(value.toInstant()).getTime());
      } else {
        writer.writeNull();
      }
    }

    @Override
    public ZonedDateTime decode(final BsonReader reader, final DecoderContext decoderContext) {
      BsonType type = reader.getCurrentBsonType();

      if (type == BsonType.NULL) {
        reader.readNull();
        return null;
      } else if (type == BsonType.DATE_TIME) {
        long milliseconds = reader.readDateTime();
        Instant instant = Instant.ofEpochMilli(milliseconds);
        return ZonedDateTime.ofInstant(instant, DateTimeUtils.SYSTEM_ZONE_ID);
      } else {
        throw new UnsupportedOperationException("Unsupported BSON type for ZonedDateTime: " + type);
      }
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass() {
      return ZonedDateTime.class;
    }
  }
}
