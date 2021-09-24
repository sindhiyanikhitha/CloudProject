# CSYE 6225 - Spring 2020


| Name | NEU ID | Email Address |
| --- | --- | --- |
| Nikhitha Sindhiya | 001496695| sindhiya.ni@husky.neu.edu |
| | | |

## Technology Stack
- Programming Language: Java 1.11
- Web Framework: Springboot 2.2.3.RELEASE
- Database: MySql
- IDE: IntelliJ
- Version Control: Git
- Project Management: Maven
- Test Tool: Postman
- Development Environment: Ubuntu

## Build Instructions
Clone the repository into a local repository

Use Maven to build:
<code>$ mvn clean install</code>

run the application by executing:
<code>$ java -jar target/demo-0.0.1-SNAPSHOT.jar</code>

The server will be run at http://localhost:8080/, test can be done using Postman.

## Deploy Instructions
MySQL port is default 3306.

Server: server side as RESTful architectural style. As a default, it is listening at http://localhost:8080/


## Running Tests
Our test files are in the file src/test, all the functional tests and module tests are included in this file.

## CI/CD
Circle CI for continuous Integration checks



