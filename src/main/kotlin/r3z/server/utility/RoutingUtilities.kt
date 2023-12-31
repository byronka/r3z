package r3z.server.utility

import r3z.authentication.api.ChangePasswordAPI
import r3z.authentication.api.LoginAPI
import r3z.authentication.api.LogoutAPI
import r3z.authentication.api.RegisterAPI
import r3z.system.logging.LoggingAPI
import r3z.server.api.HomepageAPI
import r3z.server.api.MessageAPI
import r3z.server.api.handleNotFound
import r3z.server.types.PreparedResponseData
import r3z.server.types.ServerData
import r3z.server.types.Verb
import r3z.timerecording.api.*


class RoutingUtilities {

    companion object {
        /**
         * Examine the request and headers, direct the request to a proper
         * point in the system that will take the proper action, returning a
         * proper response with headers.
         *
         * Register your endpoints here
         */
        fun routeToEndpoint(sd: ServerData): PreparedResponseData {
            return when (Pair(sd.ahd.verb, sd.ahd.path)) {
                // GET

                Pair(Verb.GET, ""),
                Pair(Verb.GET, HomepageAPI.path) -> HomepageAPI.handleGet(sd)
                Pair(Verb.GET, ViewTimeAPI.path) -> ViewTimeAPI.handleGet(sd)
                Pair(Verb.GET, CreateEmployeeAPI.path) -> CreateEmployeeAPI.handleGet(sd)
                Pair(Verb.GET, LoginAPI.path) -> LoginAPI.handleGet(sd)
                Pair(Verb.GET, RegisterAPI.path) -> RegisterAPI.handleGet(sd)
                Pair(Verb.GET, ProjectAPI.path) -> ProjectAPI.handleGet(sd)
                Pair(Verb.GET, LogoutAPI.path) -> LogoutAPI.handleGet(sd)
                Pair(Verb.GET, LoggingAPI.path) -> LoggingAPI.handleGet(sd)
                Pair(Verb.GET, ChangePasswordAPI.path) -> ChangePasswordAPI.handleGet(sd)
                Pair(Verb.GET, MessageAPI.path) -> MessageAPI.handleGet(sd)

                // POST

                Pair(Verb.POST, EnterTimeAPI.path) -> EnterTimeAPI.handlePost(sd)
                Pair(Verb.POST, CreateEmployeeAPI.path) -> CreateEmployeeAPI.handlePost(sd)
                Pair(Verb.POST, LoginAPI.path) -> LoginAPI.handlePost(sd)
                Pair(Verb.POST, RegisterAPI.path) -> RegisterAPI.handlePost(sd)
                Pair(Verb.POST, ProjectAPI.path) -> ProjectAPI.handlePost(sd)
                Pair(Verb.POST, DeleteProjectAPI.path) -> DeleteProjectAPI.handlePost(sd)
                Pair(Verb.POST, DeleteEmployeeAPI.path) -> DeleteEmployeeAPI.handlePost(sd)
                Pair(Verb.POST, LoggingAPI.path) -> LoggingAPI.handlePost(sd)
                Pair(Verb.POST, DeleteTimeAPI.path) -> DeleteTimeAPI.handlePost(sd)
                Pair(Verb.POST, SubmitTimeAPI.path) -> SubmitTimeAPI.handlePost(sd)
                Pair(Verb.POST, ChangePasswordAPI.path) -> ChangePasswordAPI.handlePost(sd)
                Pair(Verb.POST, ApproveApi.path) -> ApproveApi.handlePost(sd)
                Pair(Verb.POST, RoleAPI.path) -> RoleAPI.handlePost(sd)

                else -> {
                    handleNotFound()
                }
            }
        }
    }
}