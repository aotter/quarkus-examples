package net.aotter

import io.quarkus.hibernate.reactive.panache.PanacheRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class FruitRepository : PanacheRepository<FruitPO>{

}