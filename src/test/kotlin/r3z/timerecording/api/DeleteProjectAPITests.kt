package r3z.timerecording.api

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
import r3z.timerecording.types.DeleteProjectResult
import r3z.timerecording.types.NO_PROJECT
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class DeleteProjectAPITests {
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
    fun testDeleteProject() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.PROJECT_DELETED)
        tru.findProjectByIdBehavior = { DEFAULT_PROJECT }
        val data = PostBodyData(
            mapOf(
                DeleteProjectAPI.Elements.ID.getElemName() to "1"
            )
        )
        val sd = makeDPServerData(data)

        val response = DeleteProjectAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    @Test
    fun testDeleteProject_NonNumericId() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "Must be able to parse \"abc\" as an integer",
            false,
            ProjectAPI.path)

        val data = PostBodyData(mapOf(
            DeleteProjectAPI.Elements.ID.getElemName() to "abc"
        ))
        val sd = makeDPServerData(data)

        val result = DeleteProjectAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If we send an ID for a project that doesn't exist
     */
    @Test
    fun testDeleteProject_ProjectNotFoundById() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No project found by that id",
            false,
            ProjectAPI.path)

        tru.findProjectByIdBehavior = { NO_PROJECT }
        val data = PostBodyData(mapOf(
            DeleteProjectAPI.Elements.ID.getElemName() to "123"
        ))
        val sd = makeDPServerData(data)

        val result = DeleteProjectAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If we send an ID for a project that is currently used,
     * then we cannot delete it
     */
    @Test
    fun testDeleteProject_ProjectUsed() {
        val expected = MessageAPI.createEnumMessageRedirect(MessageAPI.Message.PROJECT_USED)
        tru.deleteProjectBehavior = { DeleteProjectResult.USED }
        tru.findProjectByIdBehavior = { DEFAULT_PROJECT }
        val data = PostBodyData(mapOf(
            DeleteProjectAPI.Elements.ID.getElemName() to "123"
        ))
        val sd = makeDPServerData(data)

        val response = DeleteProjectAPI.handlePost(sd)

        assertEquals(expected, response)
    }

    // if we are missing the id, get an exception
    @Test
    fun testDeleteProject_MissingId() {
        val data = PostBodyData(emptyMap())
        val sd = makeDPServerData(data)

        val ex = assertThrows(InexactInputsException::class.java) { DeleteProjectAPI.handlePost(sd).statusCode }
        assertEquals("expected keys: [id]. received keys: []", ex.message)
    }

    // region role tests


    @Test
    fun testDeleteProject_Roles_NotAllowed_Regular() {
        val data = PostBodyData(mapOf(
            DeleteProjectAPI.Elements.ID.getElemName() to "1"
        ))
        val sd = makeDPServerData(data, DEFAULT_REGULAR_USER)

        val response = DeleteProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }

    @Test
    fun testDeleteProject_Roles_NotAllowed_Approver() {
        val data = PostBodyData(mapOf(
            DeleteProjectAPI.Elements.ID.getElemName() to "1"
        ))
        val sd = makeDPServerData(data, DEFAULT_APPROVER_USER)

        val response = DeleteProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }


    @Test
    fun testDeleteProject_Roles_NotAllowed_System() {
        val data = PostBodyData(mapOf(
            DeleteProjectAPI.Elements.ID.getElemName() to "1"
        ))
        val sd = makeDPServerData(data, SYSTEM_USER)

        val response = DeleteProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, response)
    }

    // endregion

    /**
     * Helper method to make a [ServerData] for the delete project API tests
     */
    private fun makeDPServerData(data: PostBodyData, user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = DeleteProjectAPI.path)
    }
}