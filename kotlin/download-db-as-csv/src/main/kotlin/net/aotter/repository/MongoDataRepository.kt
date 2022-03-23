package net.aotter.repository

import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.Indexes
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import net.aotter.model.MongoData
import org.jboss.logging.Logger
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

}
