package r3z.authentication.api

import r3z.authentication.types.NO_USER
import r3z.authentication.types.SYSTEM_USER
import r3z.authentication.types.User
import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.system.misc.DEFAULT_ADMIN_USER
import r3z.system.misc.makeServerData
import r3z.server.APITestCategory
import r3z.server.types.PostBodyData
import r3z.server.types.ServerData
import r3z.server.types.StatusCode
import r3z.timerecording.FakeTimeRecordingUtilities
import r3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class LogoutAPITests {


    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * Basic happy path - they were logged in, they are logging out
     */
    @Test
    fun testLogout() {
        val sd = makeLogoutServerData()

        val result = LogoutAPI.handleGet(sd).fileContentsString()

        assertTrue(result.contains("You are now logged out"))
    }

    /**
     * The System role is not allowed to log out
     */
    @Test
    fun testLogout_RolesDisallowed_System() {
        val sd = makeLogoutServerData(SYSTEM_USER)

        val result = LogoutAPI.handleGet(sd).statusCode

        assertEquals(StatusCode.FORBIDDEN, result)
    }

    /**
     * The NONE role is not allowed to log out,
     */
    @Test
    fun testLogout_RolesDisallowed_None() {
        val sd = makeLogoutServerData(NO_USER)

        val result = LogoutAPI.handleGet(sd)

        assertEquals(StatusCode.SEE_OTHER, result.statusCode)
        assertEquals("Location: homepage", result.headers.single())
    }

    private fun makeLogoutServerData(user: User = DEFAULT_ADMIN_USER): ServerData {
        return makeServerData(PostBodyData(), tru, au, user = user, path = LogoutAPI.path)
    }
}