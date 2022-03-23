package net.aotter

import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.runBlocking
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

    fun loadDummyData(@Observes evt: StartupEvent) {
        // uncomment the following to populate dummy data
//        runBlocking {
//            mongoDataRepository.deleteAll().awaitSuspending()
//            repeat(1000) {
//                mongoDataRepository.persist(MongoData(Date())).awaitSuspending()
//            }
//        }
    }

}
