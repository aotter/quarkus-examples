# Quarkus Example for Large CSV Download

This project demonstrated the approach to export a large dataset from MongoDB as CSV file via streaming (chunked) HTTP response in a fully reactive manner, with low memory footprint. 
The feature was accomplished by the followings:
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
            mongoDataRepository.scroll(
                100, // batch size per page
                MongoData::createdTime.name, //name of the field to perform time comparison query
                { Date(it) }, // function to convert millis to the type of MongoData.createdTime
                Sort.ASC, // deep paging sort direction
                Filters.eq(MongoData::gender.name, "F") // query condition
            ) {
                // build data of the rows
                val page =
                    it.map { data -> with(data) { listOf(id?.toHexString(), name, city, phone, createdTime) } }
                // print this batch of data to csv streaming response in csv format
                printBatch(page)
            }
        }
    }
```

## Running the application in dev mode

```shell script
./mvnw compile quarkus:dev
```
and visit `/csv/mongo` to run the demo