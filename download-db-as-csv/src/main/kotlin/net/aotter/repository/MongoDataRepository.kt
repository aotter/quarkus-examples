package net.aotter.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.Indexes
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import net.aotter.model.MongoData
import org.jboss.logging.Logger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoDataRepository : ReactivePanacheMongoRepository<MongoData> {

    @Inject
    lateinit var logger: Logger

    init {
        createIndexes(
            IndexModel(
                Indexes.compoundIndex(
                    Indexes.ascending(MongoData::gender.name),
                    Indexes.descending(MongoData::createdTime.name)
                )
            )
        )
    }

    private fun createIndexes(vararg indexModels: IndexModel) {
        val col = mongoCollection()
        col.createIndexes(indexModels.asList())
            .onItemOrFailure()
            .transform { result, t ->
                if (t != null) {
                    logger.error("collection ${col.documentClass.simpleName} index creation failed: ${t.message}")
                } else {
                    logger.info("collection ${col.documentClass.simpleName} index created: $result")
                }
            }.subscribe().with { }
    }


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
