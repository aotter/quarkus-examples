package net.aotter.repository

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import io.quarkus.elytron.security.common.BcryptUtil
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import io.smallrye.mutiny.Uni
import net.aotter.constant.Role
import net.aotter.model.po.User
import org.jboss.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository : ReactivePanacheMongoRepository<User> {

    @Inject
    lateinit var logger: Logger

    init {
        mongoCollection().createIndex(
            Indexes.ascending(User::username.name),
            IndexOptions().unique(true)
        )

        // for test only, remove this code on production
        create("aotter", "aotter_password", mutableSetOf(Role.ADMIN)).subscribe()
            .with { logger.info("Created test user ${it.username}") }
    }


    /**
     * create new user
     */
    fun create(username: String, password: String, roles: MutableSet<String>): Uni<User> =
        persist(User(username.standardize(), BcryptUtil.bcryptHash(password), roles))


    /**
     * find user by username
     */
    fun findByUsername(username: String): Uni<User?> = find(User::username.name, username.standardize()).firstResult()


    private fun String.standardize() = trim().toLowerCase()

}
