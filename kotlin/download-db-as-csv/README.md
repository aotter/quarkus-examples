# Quarkus Example for Large CSV Download

This project demonstrates the approach of exporting large dataset from MongoDB as CSV file via streaming (chunked) HTTP response in a fully reactive manner, and with low memory footprint. 
It is accomplished by the following steps:
1. create a `scroll` function that fulfil efficient deep pagination of a dataset via comparing the timestamp of the last record from the previous page.
```kotlin
/**
 * a common pattern for deep scrolling pagination
 *
 * @param supplier Long: timeOfLastItem, List<String> excludeIds: Take these two params and return corresponding List of data
 * @param idValueGetter function to extract id from T
 * @param timeValueGetter function to extract timestamp millis value from T
 * @param handler function to invoke on each page of returned data
 * @param timeOfLastItem last timestamp of previous page to build next query
 * @param excludeIds ids of the data to be excluded to prevent duplication
 */
suspend fun <T> scroll(
    sizePerPage: Int,
    supplier: suspend (Long?, List<String>?) -> List<T>,
    idValueGetter: (T) -> String?,
    timeValueGetter: (T) -> Long,
    handler: suspend (List<T>) -> Unit,
    timeOfLastItem: Long? = null,
    excludeIds: List<String>? = null
) {

    val list = supplier.invoke(timeOfLastItem, excludeIds)

    // invoke the handler with the obtained list
    handler.invoke(list)

    // has next, run recursively
    list.takeIf { it.size == sizePerPage }?.last()?.run {
        // move to next page by obtaining time of the last item
        val toLI = timeValueGetter(this)
        // find items with the same time value and exclude them in the next page.
        // convert time value to millis before comparing
        val exIds = list.filter { timeValueGetter(it) == toLI }.mapNotNull { idValueGetter(it) }
        // prevent infinite looping
        if (exIds.size == sizePerPage) {
            throw IllegalArgumentException("Too many items with the same timeValue found. Either increase sizePerPage or use another timeValue for comparison.")
        }
        // call recursively
        scroll(sizePerPage, supplier, idValueGetter, timeValueGetter, handler, toLI, exIds)
    }
}

```
2. create extension function for `ReactivePanacheMongoRepository` that provides mongoDB-specific implementations.
```kotlin
/**
 * deep pagination via scroll approach
 *
 * @param sizePerPage number of records to return per page
 * @param timeField name of the field for time-based comparison query for deep pagination
 * @param timeValueConverter function to convert millis to the correct type of timeField value
 * @param sort sort direction
 * @param query the main query condition
 * @param handler function to call on every page
 */
suspend fun <Entity : ReactivePanacheMongoEntity> ReactivePanacheMongoRepository<Entity>.scroll(
    sizePerPage: Int,
    timeField: String,
    timeValueConverter: (Long) -> Any,
    sort: Sort,
    query: Bson? = null,
    handler: suspend (List<Entity>) -> Unit,
) {

    // build a supplier function to execute correct mongo query
    val supplier: suspend (Long?, List<String>?) -> List<Entity> = { timeOfLastItem, excludeIds ->

        val col = mongoCollection()

        // build time-comparison query, which is the heart of efficient deep pagination
        val timeComparisonQuery = timeOfLastItem?.let {
            val d = timeValueConverter(it)
            if (sort == Sort.ASC) {
                Filters.gte(timeField, d)
            } else {
                Filters.lte(timeField, d)
            }
        }

        // build exclude-by-ids query to prevent duplicated result being return in this page if timestamp collides
        val excludeIdsQuery = excludeIds?.let { ids ->
            Filters.nin("_id", ids.map { ObjectId(it) })
        }

        // build the final filter that combine the main query condition with time-comparison query and exclude-by-ids query
        val filter = listOfNotNull(query, timeComparisonQuery, excludeIdsQuery)
            .takeIf { it.isNotEmpty() }
            ?.let { Filters.and(it) }

        // determine sort direction accordingly
        val sorts = if (sort == Sort.ASC) {
            Sorts.ascending(timeField)
        } else {
            Sorts.descending(timeField)
        }

        // fetch data from mongoDB
        col.find(FindOptions().filter(filter).sort(sorts).limit(sizePerPage)).collect().asList().awaitSuspending()
    }

    // simply return ReactivePanacheMongoEntity's id field
    val idValueGetter: (Entity) -> String? = { it.id?.toHexString() }

    // get the time value via reflection and smart type casting
    val timeValueGetter: (Entity) -> Long = { ent ->
        when (val r = ent::class.memberProperties.find { it.name == timeField }?.getter?.call(ent)) {
            is Date -> r.time
            is Instant -> r.toEpochMilli()
            is Long -> r
            is ObjectId -> r.date.time
            else -> throw TypeCastException()
        }
    }

    // call the common scroll method to initiate deep pagination
    scroll(
        sizePerPage,
        supplier,
        idValueGetter,
        timeValueGetter,
        handler
    )
}

```
3. implement our own `ReactivePanacheMongoRepository` with `scroll` function

```kotlin
@Singleton
class MongoDataRepository : ReactivePanacheMongoRepository<MongoData> {

    /**
     * scroll the data by gender query
     * @param sizePerPage
     * @param gender F or M
     * @param handler function to call on page data return
     */
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

}

```


4. create a `streamCsv` function that produce a chunked HTTP response
```kotlin
abstract class BaseCSVResource {
    
    /**
     * generate csv file via streaming (chunked response)
     *
     * @param rc [RoutingContext]
     * @param fileName
     * @param header List of header row column contents
     * @param block function to call for printing csv with two functions as params - printRow: call when printing one row of data, flush: call when printing one batch of data
     */
    suspend fun streamCsv(
        rc: RoutingContext,
        fileName: String,
        header: List<String>,
        block: suspend (printBatch: suspend (List<List<Any?>>) -> Unit) -> Unit
    ): String {

        // prepare chunked response of file download
        val resp = rc.response().apply {
            putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
            putHeader(
                "Content-Disposition",
                "attachment; filename=$fileName.csv; filename*=utf-8''${
                    java.net.URLEncoder.encode(
                        fileName,
                        "UTF-8"
                    )
                }.csv"
            )
            isChunked = true
            write("\ufeff") // BOM
        }

        val sb = StringBuilder()

        val format = CSVFormat.EXCEL.builder()
            .setQuoteMode(QuoteMode.ALL)
            .setAutoFlush(true)
            .setHeader(*header.toTypedArray())
            .build()

        CSVPrinter(sb, format).use { printer ->

            val printBatch: suspend (List<List<Any?>>) -> Unit = { lines ->
                // print each line of data with default placeholder "N/A" if null
                lines.forEach { line ->
                    line.map { it ?: "N/A" }.let { printer.printRecord(it) }
                }
                // write to response chunk
                resp.write(sb.toString()).await()
                // clear the builder in order to process the next batch of data
                sb.clear()
            }

            // invoke the block and provide printRow function to be called by user
            block(printBatch)
        }

        // let resteasy jackson end the response properly
        return ""
    }

}
```

5. putting all together, combine `scroll` and `streamCsv` functions that writes a new chunk on every page of data returned from deep pagination of scroll function call.

```kotlin
@Path("/csv")
class CSVResource : BaseCSVResource() {

    @Inject
    lateinit var mongoDataRepository: MongoDataRepository

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
