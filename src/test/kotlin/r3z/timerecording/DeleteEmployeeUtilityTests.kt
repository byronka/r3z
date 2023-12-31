package r3z.timerecording

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.NO_USER
import r3z.authentication.types.SYSTEM_USER
import r3z.authentication.utility.AuthenticationUtilities
import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.authentication.utility.FakeRolesChecker
import r3z.persistence.utility.PureMemoryDatabase
import r3z.system.misc.*
import r3z.timerecording.types.DeleteEmployeeResult
import r3z.timerecording.types.NO_EMPLOYEE
import r3z.timerecording.utility.DeleteEmployeeUtility
import r3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import java.lang.IllegalArgumentException

/**
 * Tests for the utility that handles both authentication
 * and aspects from time recording
 */
class DeleteEmployeeUtilityTests {

    private lateinit var de: DeleteEmployeeUtility
    private lateinit var frc: FakeRolesChecker
    val tru = FakeTimeRecordingUtilities()
    val au = FakeAuthenticationUtilities()


    @Before
    fun init() {
        makeDeleteEmployeeUtility()
    }

    /**
     * Happy path test to delete a newly-created employee
     */
    @Test
    fun testDeleteEmployee() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.deleteEmployeeBehavior = { true }

        val result = de.deleteEmployee(DEFAULT_EMPLOYEE)

        assertEquals(DeleteEmployeeResult.SUCCESS, result)
    }

    /**
     * What about when the Employee passed in is
     * specifically [NO_EMPLOYEE]?  That would mean
     * a developer has gotten the wires crossed
     * somewhere and deserves an exception to be thrown.
     */
    @Test
    fun testDeleteEmployee_DisallowNoEmployee() {
        assertThrows(IllegalArgumentException::class.java) { de.deleteEmployee(NO_EMPLOYEE) }
    }

    /**
     * What will happen if we try deleting an employee
     * that isn't actually found in the database?
     */
    @Test
    fun testDeleteEmployee_EmployeeDoesNotExist() {
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.deleteEmployeeBehavior = { false }

        val result = de.deleteEmployee(DEFAULT_EMPLOYEE)

        assertEquals(DeleteEmployeeResult.DID_NOT_DELETE, result)
    }

    /**
     * What will happen if the employee has
     * registered a user?
     */
    @Test
    fun testDeleteEmployee_UserRegistered() {
        au.getUserByEmployeeBehavior = { DEFAULT_USER }

        val result = de.deleteEmployee(DEFAULT_EMPLOYEE)

        assertEquals(DeleteEmployeeResult.TOO_LATE_REGISTERED, result)
    }

    /**
     * Make sure that the roles are handled properly
     */
    @Test
    fun testDeleteEmployee_Roles() {
        au.getUserByEmployeeBehavior = { NO_USER }

        makeDeleteEmployeeUtility(cu = CurrentUser(DEFAULT_ADMIN_USER))
        de.deleteEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        makeDeleteEmployeeUtility(cu = CurrentUser(DEFAULT_APPROVER_USER))
        de.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        makeDeleteEmployeeUtility(cu = CurrentUser(DEFAULT_REGULAR_USER))
        de.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)
    }

    @Category(IntegrationTestCategory::class)
    @Test
    fun testDeleteEmployee_DeletesInvitation() {
        val pmd = PureMemoryDatabase.createEmptyDatabase()
        val cu = CurrentUser(DEFAULT_ADMIN_USER)
        val tru = TimeRecordingUtilities(pmd, cu, testLogger)
        val au = AuthenticationUtilities(pmd, testLogger, CurrentUser(SYSTEM_USER))
        val de = DeleteEmployeeUtility(tru, au, cu, testLogger)

        val newEmployee = tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        au.createInvitation(newEmployee)
        assertEquals(1, au.listAllInvitations().count { it.employee == newEmployee })
        de.deleteEmployee(newEmployee)
        assertTrue(au.listAllInvitations().none { it.employee == newEmployee })
    }


    private fun makeDeleteEmployeeUtility(
        cu: CurrentUser = CurrentUser(DEFAULT_ADMIN_USER)
    ) {
        frc = FakeRolesChecker()
        de = DeleteEmployeeUtility(tru, au, cu, testLogger, frc)
    }
}