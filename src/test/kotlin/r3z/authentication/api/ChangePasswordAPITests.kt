package r3z.authentication.api

import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.system.misc.DEFAULT_REGULAR_USER
import r3z.system.misc.makeServerData
import r3z.server.APITestCategory
import r3z.server.types.PostBodyData
import r3z.server.types.StatusCode
import r3z.timerecording.FakeTimeRecordingUtilities
import r3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class ChangePasswordAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : ITimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    /**
     * Happy path - all values provided as needed
     */
    @Category(APITestCategory::class)
    @Test
    fun testHandlePost_happyPath() {
        val sd = makeServerData(PostBodyData(emptyMap()), tru, au, user = DEFAULT_REGULAR_USER, path = ChangePasswordAPI.path)

        val resultStatusCode = ChangePasswordAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.OK, resultStatusCode)
    }

}