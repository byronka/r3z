package r3z.timerecording.api

import r3z.authentication.types.NO_USER
import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.server.APITestCategory
import r3z.server.api.MessageAPI
import r3z.server.types.AuthStatus
import r3z.server.types.PostBodyData
import r3z.server.types.ServerData
import r3z.system.misc.*
import r3z.system.misc.exceptions.InexactInputsException
import r3z.timerecording.FakeTimeRecordingUtilities
import r3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class RoleAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    @Test
    fun `should be able to set an employee as a regular role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: regular",
            true,
            CreateEmployeeAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "regular"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `should be able to set an employee as an approver role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: approver",
            true,
            CreateEmployeeAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `should be able to set an employee as an admin role`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "${DEFAULT_REGULAR_USER.employee.name.value} now has a role of: admin",
            true,
            CreateEmployeeAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { DEFAULT_REGULAR_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "admin"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `passing in a name that does not correspond to any employee should result in a complaint`(){
        val expected = MessageAPI.createCustomMessageRedirect(
            "No employee was found with an id of ${DEFAULT_REGULAR_USER.employee.id.value}",
            false,
            RoleAPI.path
        )
        tru.findEmployeeByIdBehavior = { NO_EMPLOYEE }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `passing in an employee who has no associated user should result in a complaint`() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No user associated with the employee named ${DEFAULT_REGULAR_USER.employee.name.value} and id ${DEFAULT_REGULAR_USER.employee.id.value}",
            false,
            RoleAPI.path
        )
        tru.findEmployeeByIdBehavior = { DEFAULT_EMPLOYEE }
        au.getUserByEmployeeBehavior = { NO_USER }
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = RoleAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun `should throw an exception if the client does not pass in the role`() {
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.EMPLOYEE_ID.getElemName() to DEFAULT_REGULAR_USER.employee.id.value.toString(),
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = assertThrows(InexactInputsException::class.java) { RoleAPI.handlePost(sd) }

        assertEquals("expected keys: [employee_id, role]. received keys: [employee_id]", result.message)
    }

    @Test
    fun `should throw an exception if the client does not pass in the employee id`() {
        val data = PostBodyData(
            mapOf(
                RoleAPI.Elements.ROLE.getElemName() to "approver"
            )
        )
        val sd = makeSetApproverServerData(data)

        val result = assertThrows(InexactInputsException::class.java) { RoleAPI.handlePost(sd) }

        assertEquals("expected keys: [employee_id, role]. received keys: [role]", result.message)
    }


    /**
     * A helper method for the ordinary [ServerData] present during login
     */
    private fun makeSetApproverServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, AuthStatus.AUTHENTICATED, user = DEFAULT_ADMIN_USER, path = RoleAPI.path)
    }
}