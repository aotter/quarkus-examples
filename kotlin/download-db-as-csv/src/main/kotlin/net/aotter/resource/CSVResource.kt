package net.aotter.resource

import io.vertx.ext.web.RoutingContext
import net.aotter.repository.MongoDataRepository
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path

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