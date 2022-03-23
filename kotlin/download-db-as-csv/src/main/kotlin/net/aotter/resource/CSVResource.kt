package net.aotter.resource

import io.vertx.ext.web.RoutingContext
import net.aotter.model.MongoData
import net.aotter.repository.MongoDataRepository
import net.aotter.repository.Sort
import net.aotter.repository.scroll
import org.jboss.logging.Logger
import java.util.*
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/csv")
class CSVResource : BaseCSVResource() {

    @Inject
    lateinit var mongoDataRepository: MongoDataRepository

    @Inject
    lateinit var logger: Logger


    @GET
    @Path("/mongo")
    suspend fun downloadMongoData(rc: RoutingContext) =
        streamCsv(rc, "mongo-data", listOf("id", "createdTime")) { printBatch ->
            mongoDataRepository.scroll(10, MongoData::createdTime.name, { Date(this) }, Sort.ASC) {
                val page = it.map { data -> listOf(data.id?.toHexString(), data.createdTime.time) }
                printBatch(page)
            }
        }


}