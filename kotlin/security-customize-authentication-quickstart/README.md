CustomAuthenticationMechanism implements HttpAuthenticationMechanism

getCredentialType

The credential transport, used to make sure multiple incompatible mechanisms are not installed May be null if this mechanism cannot interfere with other mechanisms

| type | type target |
|------|-------------|
|COOKIE|cookie name|
|AUTHORIZATION|auth type (basic, bearer...)|
|OTHER_HEADER|header name|
|POST|post uri|

fun main(){
// di87Z0pMbCVj6D9qyRTPLUAua-8
val aotter = "YW90dGVy"
val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(HmacUtils(HmacAlgorithms.HMAC_SHA_1, "q72aHXnaYjuGSDMQ").hmac(aotter))
println(encoded)
}
