import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.aotter.PageRequest
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.io.InputStream
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class MainResource {

    /**
     * Possible exceptions:
     * - com.fasterxml.jackson.databind.exc.InvalidDefinitionException
     *      Any request because Jackson cannot create an instance of PageRequest without the help from jackson-module-kotlin
     *
     * Input: (Anything)
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.InvalidDefinitionException",
     *           "message": "Cannot construct instance of `net.aotter.playground.jackson.resources.PageRequest`
     *                       (no Creators, like default constructor, exist): cannot deserialize from Object value
     *                       (no delegate- or property-based Creator)..." }
     */
    @POST
    @Path("/bare")
    fun jacksonWithoutKotlinModule(bodyInputStream: InputStream): RestResponse<String> {
        val mapper = ObjectMapper()
        try {
            val request: PageRequest = mapper.readValue(bodyInputStream, PageRequest::class.java)
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("search", request.search)
                    .put("page", request.page)
                    .put("show", request.show)
                    .put("beforeTimestamp", request.beforeTimestamp)
            )
            return RestResponse.ok(responseBody)
        }
        catch (e: Exception) {
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("exception", e::class.java.canonicalName)
                    .put("message", e.message)
            )
            return RestResponse.status(RestResponse.Status.BAD_REQUEST, responseBody)
        }
    }

    /**
     * Possible exceptions:
     * - com.fasterxml.jackson.databind.exc.MismatchedInputException
     *      When request body is empty or not a valid JSON
     * - com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
     *      When request body contains unrecognized properties
     *
     * Input: {}
     * Output: { "search": null, "page": 0, "show": 20, "beforeTimestamp": null }
     *
     * Input: null
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "No content to map due to end-of-input..." }
     *
     * Input: { "search": "text", "page": 2, "show": 5 }
     * Output: { "search": "text", "page": 2, "show": 5, beforeTimestamp: null }
     *
     * Input: { "hello": "world" }
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException",
     *           "message": "Unrecognized field \"hello\"..." }
     *
     * Input: { "hello": "world", "page": 1 }
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException",
     *           "message": "Unrecognized field \"hello\"..." }
     */
    @POST
    @Path("/default")
    fun jacksonDefaultBehavior(bodyInputStream: InputStream): RestResponse<String> {
        val mapper = jacksonObjectMapper()
        try {
            val request: PageRequest = mapper.readValue(bodyInputStream, PageRequest::class.java)
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("search", request.search)
                    .put("page", request.page)
                    .put("show", request.show)
                    .put("beforeTimestamp", request.beforeTimestamp)
            )
            return RestResponse.ok(responseBody)
        }
        catch (e: Exception) {
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("exception", e::class.java.canonicalName)
                    .put("message", e.message)
            )
            return RestResponse.status(RestResponse.Status.BAD_REQUEST, responseBody)
        }
    }

    /**
     * Possible exceptions:
     * - com.fasterxml.jackson.databind.exc.MismatchedInputException
     *      When primitive properties got null from request, or unknown properties from request
     *
     * Input: {}
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "Missing required creator property 'page' (index 1)..." }
     *
     * Input: null
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "No content to map due to end-of-input..." }
     *
     * Input: { "search": "text", "page": 2, "show": 5 }
     * Output: { "search": "text", "page": 2, "show": 5, "beforeTimestamp": null }
     *
     * Input: { "hello": "world" }
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "Missing required creator property 'page' (index 1)..." }
     *
     * Input: { "hello": "world", "page": 1 }
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException",
     *           "message": "Unrecognized field \"hello\"..." }
     */
    @POST
    @Path("/nullPrimitive")
    fun jacksonNoDefaultForPrimitive(bodyInputStream: InputStream): RestResponse<String> {
        val mapper = jacksonObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        try {
            val request: PageRequest = mapper.readValue(bodyInputStream, PageRequest::class.java)
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("search", request.search)
                    .put("page", request.page)
                    .put("show", request.show)
                    .put("beforeTimestamp", request.beforeTimestamp)
            )
            return RestResponse.ok(responseBody)
        }
        catch (e: Exception) {
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("exception", e::class.java.canonicalName)
                    .put("message", e.message)
            )
            return RestResponse.status(Response.Status.BAD_REQUEST, responseBody)
        }
    }

    /**
     * Possible exceptions:
     * - com.fasterxml.jackson.databind.exc.MismatchedInputException
     *      When primitive properties got null from request, or unknown properties from request
     *
     * Input: {}
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "Missing required creator property 'page' (index 1)..." }
     *
     * Input: null
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "No content to map due to end-of-input..." }
     *
     * Input: { "search": "text", "page": 2, "show": 5 }
     * Output: { "search": "text", "page": 2, "show": 5, "beforeTimestamp": null }
     *
     * Input: { "hello": "world" }
     * Output: { "exception": "com.fasterxml.jackson.databind.exc.MismatchedInputException",
     *           "message": "Missing required creator property 'page' (index 1)..." }
     *
     * Input: { "hello": "world", "page": 1 }
     * Output: { "search": null, "page": 1, "show": 20, "beforeTimestamp": null }
     */
    @POST
    @Path("/bestConfig")
    fun jacksonRelaxedConfig(bodyInputStream: InputStream): RestResponse<String> {
        val mapper = jacksonObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        try {
            val request: PageRequest = mapper.readValue(bodyInputStream, PageRequest::class.java)
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("search", request.search)
                    .put("page", request.page)
                    .put("show", request.show)
                    .put("beforeTimestamp", request.beforeTimestamp)
            )
            return RestResponse.ok(responseBody)
        }
        catch (e: Exception) {
            val responseBody = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("exception", e::class.java.canonicalName)
                    .put("message", e.message)
            )
            return RestResponse.status(RestResponse.Status.BAD_REQUEST, responseBody)
        }
    }

    @Inject
    lateinit var objectMapper: ObjectMapper

    /**
     * (Same response as /bestConfig)
     */
    @POST
    @Path("/autoBind")
    fun jacksonAutoBind(request: PageRequest): JsonNode {
        return objectMapper.createObjectNode()
            .put("search", request.search)
            .put("page", request.page)
            .put("show", request.show)
    }

    @ServerExceptionMapper
    fun handleMismatchedInputException(e: MismatchedInputException): RestResponse<JsonNode> {
        return RestResponse.status(
            RestResponse.Status.BAD_REQUEST,
            objectMapper.createObjectNode()
                .put("exception", e::class.java.canonicalName)
                .put("message", e.message)
        )
    }

}



