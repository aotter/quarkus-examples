package net.aotter.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.quarkus.hibernate.reactive.panache.PanacheRepository
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


/**
 * TODO: provide JPA example
 */
suspend fun <Entity> PanacheRepository<Entity>.scroll(){

}


enum class Sort {
    ASC, DESC
}
