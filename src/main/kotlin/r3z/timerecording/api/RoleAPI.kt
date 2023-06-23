package r3z.timerecording.api

import r3z.authentication.types.NO_USER
import r3z.authentication.types.Role
import r3z.authentication.types.User
import r3z.server.api.MessageAPI
import r3z.server.types.*
import r3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import r3z.timerecording.types.Employee
import r3z.timerecording.types.EmployeeId
import r3z.timerecording.types.NO_EMPLOYEE
import java.util.*

class RoleAPI {

    enum class Elements(private val elemName: String, private val id: String) : Element  {
        EMPLOYEE_ID("employee_id", "employee_id"),
        ROLE("role", "role");

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
            return doPOSTAuthenticated(sd, requiredInputs, path, Role.SYSTEM, Role.ADMIN) { setApprover(sd) }
        }

        private fun setApprover(sd: ServerData): PreparedResponseData {
            val (employee, user) = obtainEmployeeAndUser(sd)
            val role = obtainRole(sd)
            sd.bc.au.addRoleToUser(user, role)
            return MessageAPI.createCustomMessageRedirect(
                "${employee.name.value} now has a role of: ${role.toString().lowercase(Locale.getDefault())}",
                true,
                CreateEmployeeAPI.path
            )
        }

        private fun obtainRole(sd: ServerData): Role {
            val roleString = sd.ahd.data.mapping[Elements.ROLE.getElemName()]?.trim() ?: ""
            return Role.valueOf(roleString.uppercase(Locale.getDefault()))
        }

        /**
         * Takes the employee id sent, finds its employee, then
         * finds the user for that employee.
         */
        private fun obtainEmployeeAndUser(sd: ServerData): Pair<Employee, User> {
            val employeeIdString = sd.ahd.data.mapping[Elements.EMPLOYEE_ID.getElemName()]
            val employeeId = EmployeeId.make(employeeIdString)
            val employee = sd.bc.tru.findEmployeeById(employeeId)
            check(employee != NO_EMPLOYEE) { "No employee was found with an id of ${employeeId.value}" }
            val user = sd.bc.au.getUserByEmployee(employee)
            check(user != NO_USER) { "No user associated with the employee named ${employee.name.value} and id ${employee.id.value}" }
            return Pair(employee, user)
        }

        override val requiredInputs: Set<Element> = setOf(
            Elements.EMPLOYEE_ID,
            Elements.ROLE
        )

        override val path: String = "setapprover"

    }
}