package r3z.timerecording.utility

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.NO_USER
import r3z.authentication.types.Role
import r3z.authentication.utility.IAuthenticationUtilities
import r3z.authentication.utility.IRolesChecker
import r3z.authentication.utility.RolesChecker
import r3z.system.logging.ILogger
import r3z.timerecording.types.DeleteEmployeeResult
import r3z.timerecording.types.Employee
import r3z.timerecording.types.NO_EMPLOYEE

class DeleteEmployeeUtility(
    private val tru: ITimeRecordingUtilities,
    private val au: IAuthenticationUtilities,
    val cu: CurrentUser,
    private val logger: ILogger,
    private val rc : IRolesChecker = RolesChecker.Companion
) {

    fun deleteEmployee(employee: Employee): DeleteEmployeeResult {
        rc.checkAllowed(cu, Role.ADMIN)

        require(employee != NO_EMPLOYEE)

        /*
        check to make sure they haven't already registered (if so,
        we cannot delete this employee)
        */
        if (au.getUserByEmployee(employee) != NO_USER) {
            return DeleteEmployeeResult.TOO_LATE_REGISTERED
        }

        val deleteEmployeeResult = tru.deleteEmployee(employee)

        return if (deleteEmployeeResult) {
            au.removeInvitation(employee)
            logger.logAudit(cu) {"succeeded at deleting the employee \"${employee.name.value}\" with id ${employee.id.value}"}
            DeleteEmployeeResult.SUCCESS
        } else {
            logger.logWarn(cu) {"failed at deleting the employee \"${employee.name.value}\" with id ${employee.id.value}"}
            DeleteEmployeeResult.DID_NOT_DELETE
        }
    }

}