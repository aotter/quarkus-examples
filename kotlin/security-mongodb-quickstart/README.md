# security-mongodb-quickstart

This is the missing documentation of Quarkus regarding [form based authentication](https://quarkus.io/guides/security-built-in-authentication#form-auth). 
It took us a while to figure out how it works. Here we provide a workable demo and hope that you find it useful.

### In this project:
1. Implemented IdentityProvider for MongoDB
2. Use BCrypt for password hashing
3. Use Reactive MongoDB with Panache


# Dependencies
- use java 11
- add the following to your pom.xml 

```
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-security</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elytron-security-common</artifactId>
</dependency>
```

# Configuration
```
# --- MongoDB client ---
quarkus.mongodb.connection-string = mongodb://localhost:27017
quarkus.mongodb.database = quarkus

# --- Form authentication ---
# enable form based authentication
quarkus.http.auth.form.enabled = true

# be sure to replace with env var on production
quarkus.http.auth.session.encryption-key = moXBxFqO1fUCtcYNsQAEWEjm0AXM84kgpi7HKDePq+k=
```

# Run
```
./mvnw compile quarkus:dev
```
- visit `localhost:8080/login.html`
- username: `aotter`, password: `aotter_password`
