package net.aotter.model

import com.fasterxml.jackson.annotation.JsonIgnore
import net.aotter.constant.Role

data class User(

    var username: String = "",

    @JsonIgnore
    var password: String  = "",

    /**
     * allowed values are [Role.ADMIN],  [Role.USER]
     */
    var roles: MutableSet<String> = mutableSetOf()

)