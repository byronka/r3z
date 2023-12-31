package r3z.timerecording.api

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.Role
import r3z.server.api.MessageAPI
import r3z.server.types.Element
import r3z.server.types.PostEndpoint
import r3z.server.types.PreparedResponseData
import r3z.server.types.ServerData
import r3z.server.utility.AuthUtilities
import r3z.timerecording.types.DeleteEmployeeResult
import r3z.timerecording.types.EmployeeId
import r3z.timerecording.types.NO_EMPLOYEE
import r3z.timerecording.utility.DeleteEmployeeUtility

class DeleteEmployeeAPI {

    enum class Elements(private val elemName: String, private val id: String) : Element {
        EMPLOYEE_ID("employeeid", "employeeid"),
        ;
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

    companion object : PostEndpoint {
        override fun handlePost(sd: ServerData): PreparedResponseData {
            return AuthUtilities.doPOSTAuthenticated(sd, requiredInputs, path, Role.ADMIN) {
                try {
                    val de = DeleteEmployeeUtility(sd.bc.tru, sd.bc.au, CurrentUser(sd.ahd.user), sd.logger)
                    val idString = sd.ahd.data.mapping[Elements.EMPLOYEE_ID.getElemName()]
                    val id: EmployeeId = EmployeeId.make(idString)
                    val employee = sd.bc.tru.findEmployeeById(id)
                    check(employee != NO_EMPLOYEE) { "No employee found by that id" }
                    when (de.deleteEmployee(employee)) {
                        DeleteEmployeeResult.SUCCESS -> MessageAPI.createEnumMessageRedirect(MessageAPI.Message.EMPLOYEE_DELETED)
                        DeleteEmployeeResult.TOO_LATE_REGISTERED -> MessageAPI.createEnumMessageRedirect(MessageAPI.Message.EMPLOYEE_USED)
                        DeleteEmployeeResult.DID_NOT_DELETE -> MessageAPI.createEnumMessageRedirect(MessageAPI.Message.FAILED_TO_DELETE_EMPLOYEE)
                    }
                } catch (ex: Throwable) {
                    MessageAPI.createCustomMessageRedirect(
                        ex.message ?: "",
                        false,
                        CreateEmployeeAPI.path)
                }
            }
        }

        override val requiredInputs: Set<Element> = setOf(Elements.EMPLOYEE_ID)
        override val path: String = "deleteemployee"

    }
}