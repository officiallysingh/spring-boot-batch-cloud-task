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
* Reads Credit card accounts from a MongoDB collection and partition on these account numbers for high performance.
* Reads transactions from another MongoDB collection using pagination. Aggregates transaction amounts per day.
* Processes the date-wise transaction amount and writes the output to another MongoDB collection.
* It is fault-tolerant i.e. try to recover from transient failures and skip bad records. 
* It supports restartability from last failure point.

## Installation
Clone this repository, import in your favourite IDE as either Maven or Gradle project.
Built on Java 21, Spring boot 3.2.0+ and Spring batch 5.1.0+.

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
> [!IMPORTANT] Make sure **flyway** is enabled as Spring Batch and Spring Cloud Task needs their schema to be created.
Used internally by the framework to persist and retrieve metadata about the jobs and tasks.

### Sample Data


