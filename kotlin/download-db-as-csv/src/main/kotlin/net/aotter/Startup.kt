package net.aotter

import com.thedeanda.lorem.LoremIpsum
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.infrastructure.Infrastructure
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import net.aotter.model.MongoData
import net.aotter.repository.MongoDataRepository
import java.util.*
import javax.enterprise.event.Observes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Startup {

    @Inject
    lateinit var mongoDataRepository: MongoDataRepository

    @Suppress("DeferredResultUnused")
    fun loadDummyData(@Observes evt: StartupEvent) {
        // populate dummy data async
        GlobalScope.async(Infrastructure.getDefaultExecutor().asCoroutineDispatcher()) {
            val lorem = LoremIpsum.getInstance()
            mongoDataRepository.deleteAll().awaitSuspending()
            val currentTimeMillis = System.currentTimeMillis()
            val list = (1..100000).map { n ->
                val (gender, name) = if (n % 2 == 1) {
                    Pair("M", lorem.nameMale)
                } else {
                    Pair("F", lorem.nameFemale)
                }
                MongoData(
                    gender,
                    name,
                    lorem.city,
                    lorem.phone,
                    Date(currentTimeMillis + n)
                )
            }
            mongoDataRepository.mongoCollection().insertMany(list).awaitSuspending()
        }
    }

}
