# order-service

Spring Boot order service that manages orders, persists them in MySQL, and integrates with RabbitMQ to publish payment requests and consume payment status updates.

## Overview

This project is a Java 17 Spring Boot application built with Maven and packaged as a Docker image. The Maven configuration shows Spring Boot 4.0.6, JPA, JDBC, Web MVC, validation, Actuator, AMQP, MySQL, MapStruct, dotenv support, and Log4j2-based logging in the dependency set.

From the source layout and configuration classes, the service acts as the order domain in a microservice flow: it exposes HTTP endpoints, writes order data to a relational database, sends payment requests through RabbitMQ, and listens for payment result events to update order state.

## Stack

The repository uses Java 17, Maven Wrapper, Spring Boot, Spring Data JPA, Spring Web MVC, Spring AMQP, MySQL, RabbitMQ, MapStruct, Lombok, and Log4j2. The included `Dockerfile` packages the application so it can be run in a containerized environment.

## Project structure

```text
.
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java/com/its/gestioneordinirestclient
    │   │   ├── config
    │   │   ├── controller
    │   │   ├── dto
    │   │   ├── entity
    │   │   ├── mapper
    │   │   ├── repository
    │   │   ├── service
    │   │   └── utility
    │   └── resources
    └── test
```

The package structure indicates a standard layered design with configuration, controllers, DTOs, entities, mappers, repositories, and services separated by responsibility.

## Messaging flow

The service publishes payment request messages to the orders exchange using the `order.payment.request` routing key, and it consumes payment result events from a dedicated payment-results queue bound to a payment-results exchange. JSON serialization is configured through a Jackson message converter, which allows application DTOs to be sent and received as structured AMQP payloads.

A simplified flow looks like this:

1. A client creates or updates an order through the HTTP API.
2. The service persists the order in MySQL.
3. The service publishes a payment request event to RabbitMQ.
4. A payment service processes the request.
5. The order service consumes the payment status event and updates the order state.
