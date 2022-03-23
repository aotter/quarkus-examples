package net.aotter.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.quarkus.mongodb.FindOptions
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*
import kotlin.reflect.full.memberProperties


/**
 *
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
        // move to next page
        val toLI = timeValueGetter(this)
        // convert to millis before comparing
        val exIds = list.filter { timeValueGetter(it) == toLI }.mapNotNull { idValueGetter(it) }
        scroll(sizePerPage, supplier, idValueGetter, timeValueGetter, handler, toLI, exIds)
    }
}


/**
 * deep pagination via scroll approach
 *
 * @param sizePerPage number of records to return per page
 * @param timeField name of the field for time-based comparison query for deep pagingation
 * @param timeValueConverter function to convert millis to the value type of timeField
 * @param sort sort direction
 * @param query filter condition to perform
 * @param handler function to call per page
 */
suspend fun <Entity : ReactivePanacheMongoEntity> ReactivePanacheMongoRepository<Entity>.scroll(
    sizePerPage: Int,
    timeField: String,
    timeValueConverter: Long.() -> Any,
    sort: Sort,
    query: Bson? = null,
    handler: suspend (List<Entity>) -> Unit,
) {

    val supplier: suspend (Long?, List<String>?) -> List<Entity> = { timeOfLastItem, excludeIds ->

        val col = mongoCollection()

        val timeComparisonQuery = timeOfLastItem?.let {
            val d = it.timeValueConverter()
            if (sort == Sort.ASC) {
                Filters.gte(timeField, d)
            } else {
                Filters.lte(timeField, d)
            }
        }

        val excludeIdsQuery = excludeIds?.let { ids ->
            Filters.nin("_id", ids.map { ObjectId(it) })
        }

        val filter = listOfNotNull(query, timeComparisonQuery, excludeIdsQuery)
            .takeIf { it.isNotEmpty() }
            ?.let { Filters.and(it) }

        val sorts = if (sort == Sort.ASC) {
            Sorts.ascending(timeField)
        } else {
            Sorts.descending(timeField)
        }

        col.find(FindOptions().filter(filter).sort(sorts).limit(sizePerPage)).collect().asList().awaitSuspending()
    }

    val idValueGetter: (Entity) -> String? = { it.id?.toHexString() }

    val timeValueGetter: (Entity) -> Long = { ent ->
        when (val r = ent::class.memberProperties.find { it.name == timeField }?.getter?.call(ent)) {
            is Date -> r.time
            is Instant -> r.toEpochMilli()
            is Long -> r
            is ObjectId -> r.date.time
            else -> throw TypeCastException()
        }
    }

    scroll(
        sizePerPage,
        supplier,
        idValueGetter,
        timeValueGetter,
        handler
    )
}


enum class Sort {
    ASC, DESC
}
