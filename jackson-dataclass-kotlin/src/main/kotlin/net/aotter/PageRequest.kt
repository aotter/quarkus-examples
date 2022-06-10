package net.aotter

data class PageRequest(
    val search: String?,
    val page: Int,
    val show: Int = 20,
    val beforeTimestamp: Long?
)