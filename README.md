# Gestione Ordini REST Client — RabbitMQ

Simple Spring Boot project for managing orders using REST APIs and RabbitMQ for asynchronous communication.

## Features

* REST client integration
* RabbitMQ messaging
* Asynchronous order processing
* JSON support
* Spring Boot backend

## Tech Stack

* Java
* Spring Boot
* RabbitMQ
* Maven

## Run the Project

Clone the RabbitMQ branch:

```
git clone -b rabbitmq https://github.com/Shiinoqt/gestione-ordini-restclient.git
```

Enter the folder:

```
cd gestione-ordini-restclient
```

Start RabbitMQ with Docker:

```
docker run -d --hostname rabbitmq --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
```

Run the application:

```
mvn spring-boot:run
```

## Configuration

Edit:

```
src/main/resources/application.properties
```

Example:

```
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```
