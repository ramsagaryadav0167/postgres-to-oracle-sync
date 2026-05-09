# PostgreSQL to Oracle Sync

A Spring Boot microservice for scheduled and manual data synchronization between PostgreSQL and Oracle databases using JDBC.

---

# Features

- PostgreSQL to Oracle data transfer
- Scheduled sync process
- Manual trigger API
- JDBC Template based implementation
- Multi database configuration
- Spring Boot Scheduler support
- Maven based project

---

# Tech Stack

- Java 21
- Spring Boot 3
- Maven
- JDBC Template
- PostgreSQL
- Oracle Database
- Lombok

---

# Project Structure

src/main/java/com/icms/icmsTransfer

├── config

├── controller

├── scheduler

├── service

└── IcmsTransferApplication.java

---

# Database Flow

PostgreSQL → Spring Boot → Oracle Database

---

# API Endpoints

## Trigger Manual Data Transfer

```http
GET /transfer/run
```

### Example

```http
http://localhost:8080/transfer/run
```

---

# Configuration

Update database credentials in:

```properties
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
src/main/resources/application-uat.properties
```

Example:

```properties
spring.datasource.source.jdbc-url=YOUR_SOURCE_DB_URL
spring.datasource.source.username=YOUR_USERNAME
spring.datasource.source.password=YOUR_PASSWORD

spring.datasource.target.jdbc-url=YOUR_TARGET_DB_URL
spring.datasource.target.username=YOUR_USERNAME
spring.datasource.target.password=YOUR_PASSWORD
```

---

# Run Project

## Clone Repository

```bash
git clone https://github.com/ramsagaryadav0167/postgres-to-oracle-sync.git
```

## Move into Project

```bash
cd postgres-to-oracle-sync
```

## Build Project

```bash
mvn clean install
```

## Run Application

```bash
mvn spring-boot:run
```

---

# Scheduler Support

The application also supports automatic scheduled synchronization using Spring Scheduler.


# Author

Ram Sagar 

GitHub:
https://github.com/ramsagaryadav0167
