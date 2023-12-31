package r3z.timerecording.api

import r3z.authentication.types.Role
import r3z.system.misc.types.Date
import r3z.server.types.Element
import r3z.server.types.PostEndpoint
import r3z.server.types.PreparedResponseData
import r3z.server.types.ServerData
import r3z.server.utility.AuthUtilities
import r3z.server.utility.ServerUtilities
import r3z.timerecording.types.Employee
import r3z.timerecording.types.EmployeeId
import r3z.timerecording.types.NO_EMPLOYEE

class ApproveApi {

    enum class Elements (private val value: String) : Element {
        /**
         * If this is true, the user wants to unapprove
         */
        IS_UNAPPROVAL("unappr");

        override fun getId(): String {
            throw IllegalAccessError()
        }

        override fun getElemName(): String {
            return this.value
        }

        override fun getElemClass(): String {
            throw IllegalAccessError()
        }
    }

    companion object : PostEndpoint {

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return AuthUtilities.doPOSTAuthenticated(sd, requiredInputs, ViewTimeAPI.path, Role.ADMIN, Role.APPROVER) {
                val (employee, startDate, isUnapproval) = extractData(sd)

                if (isUnapproval == "true") {
                    sd.bc.tru.unapproveTimesheet(employee, startDate)
                } else {
                    sd.bc.tru.approveTimesheet(employee, startDate)
                }

                redirect(startDate, employee)
            }
        }

        private fun redirect(
            startDate: Date,
            employee: Employee
        ) = ServerUtilities.redirectTo(
            ViewTimeAPI.path + "?" +
                    ViewTimeAPI.Elements.TIME_PERIOD.getElemName() + "=" + startDate.stringValue + "&" +
                    ViewTimeAPI.Elements.REQUESTED_EMPLOYEE.getElemName() + "=" + employee.id.value.toString()
        )

        private fun extractData(sd: ServerData): Triple<Employee, Date, String?> {
            val employeeIdQueryString =
                sd.ahd.data.mapping[ViewTimeAPI.Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName()]
            val employeeId = EmployeeId.make(employeeIdQueryString)
            val employee = sd.bc.tru.findEmployeeById(employeeId)
            check(employee != NO_EMPLOYEE) { "No employee was found with an id of ${employeeId.value}" }
            val startDateQueryString = sd.ahd.data.mapping[ViewTimeAPI.Elements.TIME_PERIOD.getElemName()]
            val startDate = try {
                Date.make(startDateQueryString)
            } catch (ex: Throwable) {
                throw IllegalStateException("""The date for approving time was not interpreted as a date. You sent "$startDateQueryString".  Format is YYYY-MM-DD""")
            }
            val isUnapproval = sd.ahd.data.mapping[Elements.IS_UNAPPROVAL.getElemName()]
            return Triple(employee, startDate, isUnapproval)
        }

        override val requiredInputs: Set<Element> = setOf(
            ViewTimeAPI.Elements.EMPLOYEE_TO_APPROVE_INPUT,
            ViewTimeAPI.Elements.TIME_PERIOD,
            Elements.IS_UNAPPROVAL
        )

        override val path: String = "approve"

    }

}
