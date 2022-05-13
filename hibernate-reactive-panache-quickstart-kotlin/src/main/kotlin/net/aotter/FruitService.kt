package net.aotter

import io.quarkus.hibernate.reactive.panache.Panache
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class FruitService {

    @Inject
    lateinit var fruitRepository: FruitRepository


    suspend fun getAllFruit(): MutableList<FruitPO>? {
        return fruitRepository.findAll().list<FruitPO>().awaitSuspending()
    }

    fun createFruitUni(fruitRequestDTO: FruitRequestDTO): Uni<FruitPO>? {
        return fruitRepository.persist(FruitPO(name = fruitRequestDTO.name))
    }

    suspend fun createFruitSuspend(fruitRequestDTO: FruitRequestDTO): FruitPO? {
        return fruitRepository.persist(FruitPO(name = "Suspend-" + fruitRequestDTO.name)).awaitSuspending()
    }

    suspend fun createFruitSuspendWithPanacheTransaction(fruitRequestDTO: FruitRequestDTO): FruitPO? {
        return Panache.withTransaction {
            fruitRepository.persist(FruitPO(name = "SuspendWithPanacheTransaction-" + fruitRequestDTO.name))
        }.awaitSuspending()
    }

    suspend fun createFruitSuspendWithReactiveTransactionalAnnotation(fruitRequestDTO: FruitRequestDTO): FruitPO? {
        return fruitRepository.persist(FruitPO(name = "SuspendWithReactiveTransactionalAnnotation-" + fruitRequestDTO.name)).awaitSuspending()
    }


}