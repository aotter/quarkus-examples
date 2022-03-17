package net.aotter

import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello")
class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("ADMIN")
    fun hello() = "Hello RESTEasy"
}