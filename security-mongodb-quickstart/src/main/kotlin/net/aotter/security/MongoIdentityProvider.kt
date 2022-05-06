package net.aotter.security

import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest
import io.smallrye.mutiny.Uni
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.enterprise.context.ApplicationScoped


/**
 * Provide IdentityProvider to enable Quarkus Form Based Authentication (undocumented behavior).
 * Quarkus Form Based Authentication use this instance to find and authenticate user at login.
 */
@ApplicationScoped
class MongoIdentityProvider : AbstractMongoIdentityProvider(), IdentityProvider<UsernamePasswordAuthenticationRequest> {


    override fun getRequestType(): Class<UsernamePasswordAuthenticationRequest> {
        return UsernamePasswordAuthenticationRequest::class.java
    }


    override fun authenticate(
        request: UsernamePasswordAuthenticationRequest?,
        context: AuthenticationRequestContext?
    ): Uni<SecurityIdentity>? = request?.username
        ?.let { userRepository.findByUsername(it) }
        ?.onItem()
        ?.transform { user ->
            user?.takeIf { request.password?.password?.let { user.verifyPassword(it) } ?: false }
                ?.let { buildSecurityIdentity(it) }
                ?: throw AuthenticationFailedException()
        }

}