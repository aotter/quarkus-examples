package net.aotter.model

import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity
import java.util.*

/**
 * Note: we use no-arg plugin on [MongoEntity]
 */
@MongoEntity
data class MongoData(

    var gender: String,

    var name: String,

    var city: String,

    var phone: String,

    var createdTime: Date

) : ReactivePanacheMongoEntity()
