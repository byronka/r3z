package r3z.server.types

import r3z.authentication.types.NO_USER
import r3z.authentication.types.User

/**
 * Encapsulates the proper action by the server, based on what
 * the client wants from us
 */
data class AnalyzedHttpData(
    val verb: Verb = Verb.NONE,
    val path: String = "(NOTHING REQUESTED)",
    val queryString: Map<String,String> = mapOf(),
    val rawQueryString: String = "",
    val data: PostBodyData = PostBodyData(),
    val user: User = NO_USER,
    val sessionToken: String = "NO TOKEN",
    val headers: List<String> = emptyList(),
    val statusCode: StatusCode = StatusCode.NONE
)