package r3z.authentication.api

import r3z.authentication.types.Password
import r3z.authentication.types.Role
import r3z.system.config.SIZE_OF_DECENT_PASSWORD
import r3z.system.misc.utility.generateRandomString
import r3z.server.api.HomepageAPI
import r3z.server.types.*
import r3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import r3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import r3z.server.utility.PageComponents
import r3z.server.utility.ServerUtilities.Companion.okHTML

class ChangePasswordAPI {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        YES_BUTTON("", "yes_button"),
        BRING_ME_BACK("", "bring_me_back_button"),
        NEW_PASSWORD("", "new_password");

        override fun getId(): String {
            return this.id
        }

        override fun getElemName(): String {
            return this.elemName
        }

        override fun getElemClass(): String {
            throw IllegalAccessError()
        }
    }

    companion object : GetEndpoint, PostEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            return doGETRequireAuth(sd.ahd.user, Role.ADMIN, Role.APPROVER, Role.REGULAR) {
                val body = """
        <div class="container">
            <form id="new_password_form" action="$path" method="post">
                <p>
                    Would you like a new password? If so, a new random password
                    will be generated for you, and the old password will not work. 
                    Be prepared to store this somewhere, like a secure password manager.
                </p>
            </form>
           
            <form id="cancel_creating_new_password_form" action="${HomepageAPI.path}" method="get">
            </form>
             <p>
                <button form="new_password_form" id="${Elements.YES_BUTTON.getId()}">Yes</button>
                <button form="cancel_creating_new_password_form" id="${Elements.BRING_ME_BACK.getId()}">No, bring me back</button>
            </p>
            
        </div>
    """.trimIndent()
                PageComponents(sd).makeTemplate("Change Password", "ChangePasswordAPI", body, extraHeaderContent="""<link rel="stylesheet" href="changepassword.css" />""")
            }
        }

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(sd, requiredInputs, HomepageAPI.path, Role.ADMIN, Role.APPROVER, Role.REGULAR) {
                val newPassword = Password(generateRandomString(SIZE_OF_DECENT_PASSWORD))
                sd.logger.logAudit { "Requested new password" }
                sd.bc.au.changePassword(sd.ahd.user, newPassword)
                val newUserHtml = """
                <div class="container">
                    <p>
                        Your new password is <span id="${Elements.NEW_PASSWORD.getId()}">${newPassword.value}</span> 
                    </p>
                    <p>
                        <em>store this somewhere</em>
                    </p>
                    <p><a href="${HomepageAPI.path}">Homepage</a></p>
                </div>
                """
                okHTML(PageComponents(sd).makeTemplate("Password Generated", "ChangePasswordAPI", newUserHtml, extraHeaderContent="""<link rel="stylesheet" href="changepassword.css" />"""))
            }
        }

        override val requiredInputs: Set<Element>
            get() = emptySet()

        override val path: String
            get() = "changepassword"

    }
}