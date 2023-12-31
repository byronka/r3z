package r3z.server.api

import r3z.authentication.types.User
import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.system.misc.DEFAULT_ADMIN_USER
import r3z.system.misc.DEFAULT_APPROVER_USER
import r3z.system.misc.DEFAULT_REGULAR_USER
import r3z.system.misc.makeServerData
import r3z.server.APITestCategory
import r3z.server.types.PostBodyData
import r3z.server.types.ServerData
import r3z.server.types.StatusCode
import r3z.timerecording.FakeTimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class HomepageAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * A basic happy path - an administrator GETing the page
     */
    @Category(APITestCategory::class)
    @Test
    fun testGetAsAdmin() {
        val sd = makeServerData(user = DEFAULT_ADMIN_USER)

        val result = HomepageAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("Employees"))
        assertTrue(result.contains("Projects"))
        assertTrue(result.contains("Time entries"))
        assertTrue(result.contains("Log configuration"))
    }

    /**
     * If a regular user asks for the homepage, they get redirected to
     * viewing time entries
     */
    @Category(APITestCategory::class)
    @Test
    fun testGetAsRegularUser() {
        val sd = makeServerData(user = DEFAULT_REGULAR_USER)

        val result = HomepageAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    /**
     * If a regular user asks for the homepage, they get redirected to
     * viewing time entries
     */
    @Category(APITestCategory::class)
    @Test
    fun testGetAsApproverUser() {
        val sd = makeServerData(user = DEFAULT_APPROVER_USER)

        val result = HomepageAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, result)
    }

    fun makeServerData(user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(user = user, data = PostBodyData(), tru = tru, au = au, path = HomepageAPI.path)
    }
}