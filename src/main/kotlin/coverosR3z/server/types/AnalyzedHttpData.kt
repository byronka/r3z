package coverosR3z.server.types

import coverosR3z.authentication.types.NO_USER
import coverosR3z.authentication.types.User

/**
 * Encapsulates the proper action by the server, based on what
 * the client wants from us
 */
data class AnalyzedHttpData(
    val verb: Verb = Verb.NONE,
    val path: String = "(NOTHING REQUESTED)",
    val data: Map<String, String> = emptyMap(),
    val user: User = NO_USER,
    val sessionToken: String = "NO TOKEN",
    val headers: List<String> = emptyList(),
    val rawData: String? = null,
    val statusCode: StatusCode = StatusCode.NONE
)