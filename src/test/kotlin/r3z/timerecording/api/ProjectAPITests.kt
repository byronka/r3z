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
import r3z.timerecording.types.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class ProjectAPITests {


    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A basic happy path
     */
    @Test
    fun testHandlePOSTNewProject() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makePServerData(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Huge name
     */
    @Test
    fun testHandlePOSTNewProject_HugeName() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "Max size of project name is $maxProjectNameSize",
            false,
            ProjectAPI.path)
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(maxProjectNameSize + 1)))
        val sd = makePServerData(data)

        val result = ProjectAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * Big name, but acceptable
     */
    @Test
    fun testHandlePOSTNewProject_BigName() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "a".repeat(maxProjectNameSize)))
        val sd = makePServerData(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * Missing data
     */
    @Test
    fun testHandlePOSTNewProject_noBody() {
        val sd = makePServerData(PostBodyData())
        val ex = assertThrows(InexactInputsException::class.java){  ProjectAPI.handlePost(sd) }
        assertEquals("expected keys: [project_name]. received keys: []", ex.message)
    }

    // region ROLES TESTS

    /**
     * Should only allow the admin to post
     */
    @Test
    fun testShouldAllowAdminForPost() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makePServerData(data)

        assertEquals(StatusCode.SEE_OTHER, ProjectAPI.handlePost(sd).statusCode)
    }

    /**
     * There's no need for the system role to create projects
     */
    @Test
    fun testShouldDisallowSystemForPost() {
        val sd = makePServerData(PostBodyData(), user = SYSTEM_USER)

        val result = ProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow approvers to create projects
     */
    @Test
    fun testShouldDisallowApproverForPost() {
        val sd = makePServerData(PostBodyData(), user = DEFAULT_APPROVER_USER)

        val result = ProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow regular roles to create projects
     */
    @Test
    fun testShouldDisallowRegularRoleForPost() {
        val sd = makePServerData(PostBodyData(), user = DEFAULT_REGULAR_USER)

        val result = ProjectAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Only the admin can view this page
     */
    @Test
    fun testShouldAllowAdminForGet() {
        val data = PostBodyData(mapOf(ProjectAPI.Elements.PROJECT_INPUT.getElemName() to DEFAULT_PROJECT_NAME.value))
        val sd = makePServerData(data)

        assertEquals(StatusCode.OK, ProjectAPI.handleGet(sd).statusCode)
    }

    /**
     * Disallow the system role from viewing this page
     */
    @Test
    fun testShouldDisallowSystemForGet() {
        val sd = makePServerData(PostBodyData(), user = SYSTEM_USER)

        val result = ProjectAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow approvers seeing this page
     */
    @Test
    fun testShouldDisallowApproverForGet() {
        val sd = makePServerData(PostBodyData(), user = DEFAULT_APPROVER_USER)

        val result = ProjectAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * Disallow regular users viewing this page
     */
    @Test
    fun testShouldDisallowRegularRoleForGet() {
        val sd = makePServerData(PostBodyData(), user = DEFAULT_REGULAR_USER)

        val result = ProjectAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    // endregion

    /**
     * We'll trim the input - chop off the whitespace before and
     * after, and make sure to disallow duplicates from that.
     */
    @Test
    fun testShouldDisallowDuplicateNamesAfterTrimming() {
        val expected =
            MessageAPI.createEnumMessageRedirect(MessageAPI.Message.FAILED_CREATE_PROJECT_DUPLICATE)
        tru.findProjectByNameBehavior = { Project(ProjectId(1), ProjectName("abc123")) }
        val data = PostBodyData(mapOf(
            ProjectAPI.Elements.PROJECT_INPUT.getElemName() to "   abc123   "))
        val sd = makePServerData(data)
        val result = ProjectAPI.handlePost(sd)

        assertEquals(expected, result)
    }


    private fun makePServerData(data: PostBodyData, user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(data, tru, au, user = user, path = ProjectAPI.path)
    }
}