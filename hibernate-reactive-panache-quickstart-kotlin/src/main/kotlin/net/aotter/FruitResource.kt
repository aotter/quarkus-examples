package net.aotter

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.smallrye.mutiny.Uni
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path


@Path("/fruit")
class FruitResource {

    @Inject
    lateinit var fruitService: FruitService

    @GET
    suspend fun getAllFruit(): MutableList<FruitPO>? {
        return fruitService.getAllFruit()
    }

    /**
     *  NO: 1
     *  successes: Y
     */
    @POST
    @ReactiveTransactional
    fun createFruitUni(fruitRequestDTO: FruitRequestDTO): Uni<FruitPO>? {
        return fruitService.createFruitUni(fruitRequestDTO)
    }

    /**
     *  NO: 2
     *  successes: db didn't commit anything
     */
    @POST
    @Path("/suspend")
    suspend fun createFruitSuspend(fruitRequestDTO: FruitRequestDTO): FruitPO? {
        return fruitService.createFruitSuspend(fruitRequestDTO)
    }

    /**
     *  NO: 3
     *  successes: Y
     */
    @POST
    @Path("/suspend/withPanacheTransaction")
    suspend fun createFruitSuspendWithPanacheTransaction(fruitRequestDTO: FruitRequestDTO): FruitPO? {
        return fruitService.createFruitSuspendWithPanacheTransaction(fruitRequestDTO)
    }

    /**
     *  NO: 4
     *  successes: Exception: only Uni is supported when using @ReactiveTransaction if you are running on a VertxThread
     */
    @POST
    @Path("/suspend/withReactiveTransactionalAnnotation")
    @ReactiveTransactional
    suspend fun createFruitSuspendWithReactiveTransactionalAnnotation(fruitRequestDTO: FruitRequestDTO): FruitPO? {
        return fruitService.createFruitSuspendWithReactiveTransactionalAnnotation(fruitRequestDTO)
    }




}