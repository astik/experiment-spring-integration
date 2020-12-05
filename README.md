# Experiment: Spring-integration

This project aims to experiment with [spring-integration](https://spring.io/projects/spring-integration), especially with its [Java DSL](https://docs.spring.io/spring-integration/docs/current/reference/html/dsl.html#java-dsl).

To do so, we will build a simple file flow that takes files into a source folder and extract, from those input files, CSV content that we dump in a target folder.

## Idea

![UML diagram](./uml/idea.png)

There is 1 entry :

- the source folder poller

There are 2 exits :

- the dump to target folder
- the error channel

## Test me

```sh
mvn spring-boot:run
```

Once started, move a file from `data/source-example` to `data/source`.
You should see some log about what is going on in the system.
Also, you should notice some new files into `data.target`.
