# Quarkus Example for Large CSV Download

This project demonstrated the approach to export a large dataset from MongoDB as CSV file via streaming (chunked) HTTP response in a fully reactive manner. 
The feature was accomplished by the followings:
1. created a `scroll` function that fulfil efficient deep pagination of a dataset via comparing the timestamp of the last record from the previous page.
2. created a `streamCsv` function that produce a chunked HTTP response 
3. combined `scroll` and `streamCsv` functions that writes a new chunk on every page of data return from deep pagination of scroll function call.

The final outcome is as follows:


## Running the application in dev mode

```shell script
./mvnw compile quarkus:dev
```
and visit `/csv/mongo` to run the demo