package net.aotter.security

import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.TrustedAuthenticationRequest
import io.smallrye.mutiny.Uni
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.enterprise.context.ApplicationScoped

/**
 * Provide IdentityProvider to enable Quarkus Form Based Authentication (undocumented behavior).
 * Quarkus Form Based Authentication use this instance to authenticate user on every request
 */
@ApplicationScoped
class MongoTrustedIdentityProvider : AbstractMongoIdentityProvider(), IdentityProvider<TrustedAuthenticationRequest> {


    override fun getRequestType(): Class<TrustedAuthenticationRequest> {
        return TrustedAuthenticationRequest::class.java
    }


    override fun authenticate(
        request: TrustedAuthenticationRequest?,
        context: AuthenticationRequestContext?
    ): Uni<SecurityIdentity>? = request?.principal
        ?.let { userRepository.findByUsername(it) }
        ?.onItem()
        ?.transform { user ->
            user?.let { buildSecurityIdentity(it) }
                ?: throw AuthenticationFailedException()
        }


}