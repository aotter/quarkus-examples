package net.aotter.security

import io.netty.handler.codec.http.HttpResponseStatus
import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.IdentityProviderManager
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.AuthenticationRequest
import io.quarkus.security.identity.request.TrustedAuthenticationRequest
import io.quarkus.vertx.http.runtime.security.*
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import java.util.*
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped


@ApplicationScoped
class CustomAuthenticationMechanism: HttpAuthenticationMechanism {

    private lateinit var loginManager: PersistentLoginManager

    @PostConstruct
    fun init(){
        loginManager = PersistentLoginManager(
            "ascAY2H75eaS8KE4",
            "quarkus-credential",
            1 * 60 * 1000L,
            30 * 60 * 1000L
        )
    }

    override fun authenticate(
        context: RoutingContext?,
        identityProviderManager: IdentityProviderManager?
    ): Uni<SecurityIdentity> {
        val restoreResult = loginManager.restore(context)
        return loginManager.restore(context)?.let {
            if(restoreResult.principal.isNullOrEmpty()){
                customAuthenticate(identityProviderManager, context)
            } else {
                trustedAuthenticate(identityProviderManager, context, restoreResult)
            }
        } ?: run {
            customAuthenticate(identityProviderManager, context)
        }
    }

    private fun customAuthenticate(identityProviderManager: IdentityProviderManager?, context: RoutingContext?): Uni<SecurityIdentity>{
        return identityProviderManager?.authenticate(
            HttpSecurityUtils.setRoutingContextAttribute(
                CustomRequest(
                    context?.getCookie("aotter-sso")?.value,
                    context?.getCookie("aotter-sso.sig")?.value,
                ),
                context
            )
        )?.onItem()?.invoke { identity ->
            loginManager.save(identity, context, null, context?.request()?.isSSL ?: false)
            context?.response()?.statusCode = 200
        } ?: throw AuthenticationFailedException()
    }

    private fun trustedAuthenticate(
        identityProviderManager: IdentityProviderManager?,
        context: RoutingContext?,
        restoreResult: PersistentLoginManager.RestoreResult
    ): Uni<SecurityIdentity>{
        return identityProviderManager?.authenticate(
            HttpSecurityUtils.setRoutingContextAttribute(
                TrustedAuthenticationRequest(restoreResult.principal),
                context
            )
        )?.onItem()?.invoke{ identity ->
            loginManager.save(identity, context, restoreResult, context?.request()?.isSSL ?: false)
            context?.response()?.statusCode = 200
        } ?: throw AuthenticationFailedException()
    }

    override fun getChallenge(context: RoutingContext?): Uni<ChallengeData> {
        return Uni.createFrom().item(ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(), null, null))
    }

    override fun getCredentialTypes(): MutableSet<Class<out AuthenticationRequest>> {
        return Collections.singleton(CustomRequest::class.java)
    }

    override fun getCredentialTransport(): HttpCredentialTransport {
        return HttpCredentialTransport(HttpCredentialTransport.Type.COOKIE, "aotter-sso")
    }
}