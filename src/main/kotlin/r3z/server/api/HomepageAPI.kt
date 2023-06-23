package r3z.server.api

import r3z.authentication.api.LoginAPI
import r3z.authentication.exceptions.UnpermittedOperationException
import r3z.authentication.types.CurrentUser
import r3z.authentication.types.Role
import r3z.authentication.utility.RolesChecker
import r3z.server.types.AuthStatus
import r3z.server.types.GetEndpoint
import r3z.server.types.PreparedResponseData
import r3z.server.types.ServerData
import r3z.server.utility.AuthUtilities.Companion.isAuthenticated
import r3z.server.utility.PageComponents
import r3z.server.utility.ServerUtilities.Companion.okHTML
import r3z.server.utility.ServerUtilities.Companion.redirectTo
import r3z.system.logging.LoggingAPI
import r3z.system.misc.utility.safeAttr
import r3z.system.misc.utility.safeHtml
import r3z.timerecording.api.CreateEmployeeAPI
import r3z.timerecording.api.ProjectAPI
import r3z.timerecording.api.ViewTimeAPI

class HomepageAPI(private val sd: ServerData)  {

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData) : PreparedResponseData {
            val hp = HomepageAPI(sd)
            return try {
                when (isAuthenticated(sd.ahd.user)) {
                    AuthStatus.AUTHENTICATED -> {
                        val cu = CurrentUser(sd.ahd.user)
                        RolesChecker.checkAllowed(cu, Role.REGULAR, Role.APPROVER, Role.ADMIN)
                        if (cu.role in listOf(Role.REGULAR, Role.APPROVER)) {
                            redirectTo(ViewTimeAPI.path)
                        } else {
                            okHTML(hp.authHomePageHTML())
                        }
                    }
                    AuthStatus.UNAUTHENTICATED -> redirectTo(LoginAPI.path)
                }
            } catch (ex: UnpermittedOperationException) {
                handleUnauthorized(ex.message)
            }
            /**
             * This is the method for when we want to go either one direction
             * if authenticated or another if unauthenticated.  Most likely
             * example: the homepage
             */
        }

        override val path: String
            get() = "homepage"
    }

    private fun authHomePageHTML(): String {
        val body = renderHomepageBody()

        return PageComponents(sd).makeTemplate("Home", "HomepageAPI", body, extraHeaderContent="""<link rel="stylesheet" href="authhomepage.css" />""")
    }

    data class HomepageItem(val link: String, val descr: String)

    private fun renderHomepageBody(): String {
        val user = sd.ahd.user
        val allowedToSee: List<HomepageItem> = when (user.role) {
            Role.ADMIN ->
                listOf(
                    HomepageItem(CreateEmployeeAPI.path, "Employees"),
                    HomepageItem(ProjectAPI.path, "Projects"),
                    HomepageItem(ViewTimeAPI.path, "Time entries"),
                    HomepageItem(LoggingAPI.path, "Log configuration"),
                )
            Role.APPROVER,
            Role.REGULAR ->
                listOf(
                    HomepageItem(ViewTimeAPI.path, "Time entries"),
                )

            Role.SYSTEM,
            Role.NONE -> emptyList()

        }
        return """
        <nav>
            <ul>
                ${allowedToSee.joinToString("") { renderHomepageItem(it.link, it.descr) }}
            </ul>
        </nav>
        """
    }
    private fun renderHomepageItem(linkTarget: String, description: String): String {

        return """
        <li><a href="${safeAttr(linkTarget)}">${safeHtml(description)}</a></li>
        """
    }

}