package net.aotter.resource

import com.mongodb.client.model.Filters
import io.vertx.ext.web.RoutingContext
import net.aotter.model.MongoData
import net.aotter.repository.MongoDataRepository
import net.aotter.repository.Sort
import net.aotter.repository.scroll
import java.util.*
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
        return streamCsv(rc, filename, headers) { printBatch ->
            mongoDataRepository.scroll(
                10,
                MongoData::createdTime.name,
                { Date(it) },
                Sort.ASC,
                Filters.eq(MongoData::gender.name, "F")
            ) {
                val page =
                    it.map { data -> with(data) { listOf(id?.toHexString(), name, city, phone, createdTime) } }
                printBatch(page)
            }
        }
    }


}