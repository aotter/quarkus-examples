package net.aotter.model.po

import com.fasterxml.jackson.annotation.JsonIgnore
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntity
import net.aotter.constant.Role
import org.wildfly.security.password.PasswordFactory
import org.wildfly.security.password.interfaces.BCryptPassword
import org.wildfly.security.password.util.ModularCrypt

data class User(

    var username: String = "",

    @JsonIgnore
    var password: String  = "",

    /**
     * allowed values are [Role.ADMIN],  [Role.USER]
     */
    var roles: MutableSet<String> = mutableSetOf()

) : ReactivePanacheMongoEntity() {

    fun verifyPassword(passwordToVerify: CharArray): Boolean = runCatching {
        val factory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT)
        ModularCrypt.decode(password)
            ?.let { factory.translate(it) as BCryptPassword }
            ?.let { factory.verify(it, passwordToVerify) }
            ?: false
    }.getOrDefault(false)

}
