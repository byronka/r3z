package r3z.timerecording.api

import r3z.authentication.types.Role
import r3z.server.api.MessageAPI
import r3z.server.api.MessageAPI.Companion.createEnumMessageRedirect
import r3z.server.types.Element
import r3z.server.types.PostEndpoint
import r3z.server.types.PreparedResponseData
import r3z.server.types.ServerData
import r3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import r3z.timerecording.types.DeleteProjectResult
import r3z.timerecording.types.NO_PROJECT
import r3z.timerecording.types.ProjectId

class DeleteProjectAPI {

    companion object : PostEndpoint {

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(sd, requiredInputs, ProjectAPI.path, Role.ADMIN) {
                val projectId = ProjectId.make(sd.ahd.data.mapping[Elements.ID.getElemName()])
                val project = sd.bc.tru.findProjectById(projectId)
                check (project != NO_PROJECT) {"No project found by that id"}
                when(sd.bc.tru.deleteProject(project)) {
                    DeleteProjectResult.SUCCESS -> createEnumMessageRedirect(MessageAPI.Message.PROJECT_DELETED)
                    DeleteProjectResult.USED -> createEnumMessageRedirect(MessageAPI.Message.PROJECT_USED)
                }
            }
        }

        override val requiredInputs: Set<Element> = setOf(Elements.ID)
        override val path: String = "deleteproject"

    }

    enum class Elements(private val value: String = "") : Element {
        ID("id");
        override fun getId(): String {
            return this.value
        }

        override fun getElemName(): String {
            return this.value
        }

        override fun getElemClass(): String {
            return this.value
        }
    }

}
