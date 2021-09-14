package net.aotter.security

import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import net.aotter.repository.UserRepository
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import java.nio.charset.Charset
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class CustomIdentityProvider: IdentityProvider<CustomRequest> {

    @Inject
    lateinit var userRepository: UserRepository

    private val mimeDecoder = Base64.getMimeDecoder()

    private val urlEncoder = Base64.getUrlEncoder()

    override fun getRequestType(): Class<CustomRequest> {
        return CustomRequest::class.java
    }

    override fun authenticate(
        request: CustomRequest?,
        context: AuthenticationRequestContext?
    ): Uni<SecurityIdentity> {
        val token = request?.token
        val sig = request?.sig
        if(token.isNullOrEmpty() || sig.isNullOrEmpty()){
            throw AuthenticationFailedException()
        }else{
            urlEncoder
                .withoutPadding()
                .encodeToString(
                    HmacUtils(HmacAlgorithms.HMAC_SHA_1, "q72aHXnaYjuGSDMQ").hmac(token)
                )
                .let {
                    if(it == sig){
                        val username = String(mimeDecoder.decode(token), Charset.forName("UTF-8"))
                        return userRepository.findByName(username).onItem().transform { user ->
                            QuarkusSecurityIdentity.builder()
                                .setPrincipal { user?.username }
                                .addRoles(user?.roles)
                                .build()
                        }
                    }
                }
            throw AuthenticationFailedException()
        }
    }
}