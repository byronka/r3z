package r3z.timerecording.api

import r3z.authentication.types.NO_USER
import r3z.authentication.types.SYSTEM_USER
import r3z.authentication.types.User
import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.server.APITestCategory
import r3z.server.api.MessageAPI
import r3z.server.types.PostBodyData
import r3z.server.types.ServerData
import r3z.server.types.StatusCode
import r3z.system.misc.*
import r3z.system.misc.exceptions.InexactInputsException
import r3z.timerecording.FakeTimeRecordingUtilities
import r3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class DeleteEmployeeAPITests {
    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * Basic happy path
     */
    @Test
    fun testDeleteEmployee() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.EMPLOYEE_DELETED)
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    @Test
    fun testDeleteEmployee_NonNumericId() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "The employee id was not interpretable as an integer.  You sent \"abc\".",
            false,
            CreateEmployeeAPI.path)

        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "abc"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    /**
     * If we send an ID for an employee that doesn't exist
     */
    @Test
    fun testDeleteEmployee_EmployeeNotFoundById() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No employee found by that id",
            false,
            CreateEmployeeAPI.path)

        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { NO_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    /**
     * If we send an ID for an employee that a user is registered to,
     * then we cannot delete it
     */
    @Test
    fun testDeleteEmployee_UserRegisteredToEmployee() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.EMPLOYEE_USED)
        au.getUserByEmployeeBehavior = { DEFAULT_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    /**
     * If there is some inexplicable error when deleting,
     * we should get a general "failed to delete" message, a 500 status code
     */
    @Test
    fun testDeleteEmployee_GeneralDeletionFailure() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.FAILED_TO_DELETE_EMPLOYEE)
        tru.deleteEmployeeBehavior = { false }
        au.getUserByEmployeeBehavior = { NO_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data)

        val response = DeleteEmployeeAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    // if we are missing the id, get an exception
    @Test
    fun testDeleteEmployee_MissingId() {
        au.getUserByEmployeeBehavior = { DEFAULT_USER }
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        val data = PostBodyData(emptyMap())
        val sd = makeDEServerData(data)

        val ex = assertThrows(InexactInputsException::class.java) { DeleteEmployeeAPI.handlePost(sd) }
        assertEquals("expected keys: [employeeid]. received keys: []", ex.message)
    }

    // region role tests


    @Test
    fun testDeleteProject_Roles_NotAllowed_Regular() {
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data, DEFAULT_REGULAR_USER)

        val response = DeleteEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }

    @Test
    fun testDeleteProject_Roles_NotAllowed_Approver() {
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data, DEFAULT_APPROVER_USER)

        val response = DeleteEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }


    @Test
    fun testDeleteProject_Roles_NotAllowed_System() {
        val data = PostBodyData(mapOf(
            DeleteEmployeeAPI.Elements.EMPLOYEE_ID.getElemName() to "1"
        ))
        val sd = makeDEServerData(data, SYSTEM_USER)

        val response = DeleteEmployeeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }

    // endregion

    /**
     * Helper method to make a [ServerData] for the delete employee API tests
     */
    private fun makeDEServerData(data: PostBodyData, user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = DeleteProjectAPI.path)
    }
}