# hibernate-reactive-panache-quickstart-kotlin

This project is the kotlin version base on [hibernate-reactive-panache-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-reactive-panache-quickstart).


## In this project:
We try the four approaches to commit data into the database


| NO. | Return | suspend function? | transaction boundary      | successes?                                                                                               |
|-----|--------|-------------------|---------------------------|----------------------------------------------------------------------------------------------------------|
| 1   | Uni    | N                 | `@ReactiveTransactional`  | Y                                                                                                        |
| 2   | Object | Y                 | N                         | db didn't commit anything                                                                                |
| 3   | Object | Y                 | `Panache.withTransaction` | Y                                                                                                        |
| 4   | Object | Y                 | `@ReactiveTransactional`  | Exception: only `Uni` is supported when using `@ReactiveTransaction` if you are running on a VertxThread |

## Run
```
./mvnw compile quarkus:dev
```

