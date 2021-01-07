package coverosR3z.timerecording.api

import coverosR3z.timerecording.types.ProjectName
import coverosR3z.authentication.types.UserName
import coverosR3z.misc.utility.safeHtml
import coverosR3z.server.utility.successHTML
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.utility.okHTML
import coverosR3z.timerecording.utility.ITimeRecordingUtilities

class ProjectAPI {

    enum class Elements(val elemName: String, val id: String) {
        PROJECT_INPUT("project_name", "project_name"),
        CREATE_BUTTON("", "project_create_button"),
    }

    companion object {

        val requiredInputs = setOf(
            Elements.PROJECT_INPUT.elemName
        )

        fun handlePOST(tru: ITimeRecordingUtilities, data: Map<String, String>) : PreparedResponseData {
            tru.createProject(ProjectName.make(data[Elements.PROJECT_INPUT.elemName]))
            return okHTML(successHTML)
        }

        fun generateCreateProjectPage(username: UserName): String = createProjectHTML(username.value)


        private fun createProjectHTML(username : String) : String {
            return """
<!DOCTYPE html>        
<html>
    <head>
        <title>create project</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body>
        <form action="createproject" method="post">
        
            <p>
                Hello there, <span id="username">${safeHtml(username)}</span>!
            </p>
        
            <p>
                <label for="${Elements.PROJECT_INPUT.elemName}">Name:</label>
                <input name="${Elements.PROJECT_INPUT.elemName}" id="${Elements.PROJECT_INPUT.id}" type="text" />
            </p>
        
            <p>
                <button id="${Elements.CREATE_BUTTON.id}">Create new project</button>
            </p>
        
        </form>
    </body>
</html>
"""
        }
    }
}
