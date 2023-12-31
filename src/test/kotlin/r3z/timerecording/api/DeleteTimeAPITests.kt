package r3z.timerecording.api

import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.server.APITestCategory
import r3z.server.api.MessageAPI
import r3z.server.types.PostBodyData
import r3z.server.types.ServerData
import r3z.server.types.StatusCode
import r3z.system.misc.DEFAULT_REGULAR_USER
import r3z.system.misc.makeServerData
import r3z.timerecording.FakeTimeRecordingUtilities
import r3z.timerecording.types.NO_TIMEENTRY
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(APITestCategory::class)
class DeleteTimeAPITests {
    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // test deleting a time entry
    @Test
    fun testDeleteTime() {
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeDTServerData(data)

        val response = DeleteTimeAPI.handlePost(sd).statusCode

        assertEquals(StatusCode.SEE_OTHER, response)
    }

    // test deleting a non-existent time entry
    @Test
    fun testDeleteTime_BadId() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "No time entry found with that id",
            false,
            ViewTimeAPI.path)
        tru.findTimeEntryByIdBehavior = { NO_TIMEENTRY }
        val data = PostBodyData(mapOf(
            ViewTimeAPI.Elements.ID_INPUT.getElemName() to "1"
        ))
        val sd = makeDTServerData(data)

        val result = DeleteTimeAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * Helper method to make a [ServerData] for the delete time API tests
     */
    private fun makeDTServerData(data: PostBodyData): ServerData {
        return makeServerData(data, tru, au, user = DEFAULT_REGULAR_USER, path = DeleteTimeAPI.path)
    }
}