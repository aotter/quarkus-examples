package net.aotter.security

import io.quarkus.security.identity.request.BaseAuthenticationRequest

class CustomRequest(
    val token: String?,
    val sig: String?
    ): BaseAuthenticationRequest()