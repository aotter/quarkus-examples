# Quarkus Example for Large CSV Download

This project demonstrates the approach of exporting large dataset from MongoDB as CSV file via streaming (chunked) HTTP response in a fully reactive manner, and with low memory footprint. 
It is accomplished by the following steps:
1. created a `scroll` function that fulfil efficient deep pagination of a dataset via comparing the timestamp of the last record from the previous page.
2. created a `streamCsv` function that produce a chunked HTTP response 
3. combined `scroll` and `streamCsv` functions that writes a new chunk on every page of data returned from deep pagination of scroll function call.

The final outcome can be seen in `net.aotter.resource.CSVResource.downloadMongoData`
```kotlin
@GET
@Path("/mongo")
suspend fun downloadMongoData(rc: RoutingContext): String {
    val filename = "all-female"
    val headers = listOf("id", "name", "city", "phone", "createdTime")

    // initiate a csv streaming response
    return streamCsv(rc, filename, headers) { printBatch ->

        // deep paging via scrolling the whole dataset
        mongoDataRepository.scrollByGender(100, "F") {

            // build data of the rows
            val page =
                it.map { data -> with(data) { listOf(id?.toHexString(), name, city, phone, createdTime) } }

            // print this batch of data to chunked response in csv format
            printBatch(page)
        }
    }
}
```

The logic of `scrollByGender` function is:
```kotlin
suspend fun scrollByGender(sizePerPage: Int, gender: String, handler: suspend (List<MongoData>) -> Unit) {
    this.scroll(
        sizePerPage,
        MongoData::createdTime.name, //name of the field to perform time comparison query
        { Date(it) }, // function to convert millis to the type of MongoData.createdTime
        Sort.ASC, // deep paging sort direction
        Filters.eq(MongoData::gender.name, gender), // query condition
        handler
    )
}
```



## Running the application in dev mode

1. you need a mongoDB up and running
2. run the shell script

```shell script
./mvnw compile quarkus:dev
```
and visit `/csv/mongo` to run the demo

Note: the code will inject 100,000 dummy data into mongoDB on app start.
