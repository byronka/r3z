package coverosR3z.server

enum class CacheControl(value: String) {

    /**
     * See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
     *
     * This is a highly aggressive caching approach.  We're basically telling
     * anyone and everyone to cache every single dang thing, which means
     * we won't hear from the client again once they request any data
     * marked with this.
     */
    AGGRESSIVE_CACHE("public, max-age=${60 * 60 * 24 * 7}, immutable"),

    /**
     * Tell clients to cache for one minute
     */
    AGGRESSIVE_MINUTE_CACHE("public, max-age=60, immutable");

    /**
     * Returns this enum as a full header
     */
    val details = "Cache-Control: $value"
}