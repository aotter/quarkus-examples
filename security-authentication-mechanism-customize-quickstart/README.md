# security-authentication-mechanism-customize-quickstart
This is the missing documentation of Quarkus regarding [HttpAuthenticationMechanism Customization](https://quarkus.io/guides/security-customization#httpauthenticationmechanism-customization).

### Scenario


### In this project
1. Prepare class CustomAuthenticationMechanism and implement HttpAuthenticationMechanism
2. Override methods
    - authenticate()
      - The main entry of the authentication process and is response for delegating the authentication job to correspond IdentityProvider
    - getChallenge()
      - This method is called when authenticate() is failed
    - getCredentialTypes()
      - Returns the required credential types
    - getCredentialTransport()
      - Used to make sure that there are no mechanisms that return same HttpCredentialTransport are implemented
   
         | type | type target |
         |------|-------------|
         |COOKIE|cookie name|
         |AUTHORIZATION|auth type (basic, bearer...)|
         |OTHER_HEADER|header name|
         |POST|post uri|
3. Prepare class CustomIdentityProvider implement IdentityProvider to handle Single-Sign-On authentication
   - Provide data class CustomRequest and extends BaseAuthenticationRequest to define what credentials should be passed to IdentityProvider for authentication    
4. Prepare class TrustedIdentityProvider implement IdentityProvider to handle requests contain quarkus-credential

# Dependencies
- use java 11
- add the following to your pom.xml


# Run
```
./mvnw compile quarkus:dev
```
- provide cookie `aotter-sso=YW90dGVy;aotter-sso.sig=di87Z0pMbCVj6D9qyRTPLUAua-8`

