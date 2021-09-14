package net.aotter.security

import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.TrustedAuthenticationRequest
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import net.aotter.repository.UserRepository
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class TrustedIdentityProvider: IdentityProvider<TrustedAuthenticationRequest> {

    @Inject
    lateinit var userRepository: UserRepository

    override fun getRequestType(): Class<TrustedAuthenticationRequest> {
        return TrustedAuthenticationRequest::class.java
    }

    override fun authenticate(
        request: TrustedAuthenticationRequest?,
        context: AuthenticationRequestContext?
    ): Uni<SecurityIdentity> {
        return userRepository.findByName(request?.principal)
            .onItem().transform { user ->
                QuarkusSecurityIdentity.builder()
                    .setPrincipal { user?.username }
                    .addRoles(user?.roles)
                    .build()
            }
    }
}