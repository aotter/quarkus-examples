package net.aotter.model

import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity
import java.util.Date

@MongoEntity
data class MongoData(

    var createdTime: Date

) : ReactivePanacheMongoEntity()
