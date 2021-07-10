package net.aotter.security

import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import net.aotter.model.po.User
import net.aotter.repository.UserRepository
import javax.inject.Inject

/**
 * a common parent class for [MongoIdentityProvider] and [MongoTrustedIdentityProvider]
 */
abstract class AbstractMongoIdentityProvider {

    @Inject
    open lateinit var userRepository: UserRepository

    open fun buildSecurityIdentity(user: User): SecurityIdentity = QuarkusSecurityIdentity.builder()
        .setPrincipal { user.username }
        .apply { user.roles.forEach { addRole(it) } }
        .build() as SecurityIdentity

}