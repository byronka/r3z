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
import r3z.timerecording.types.ApprovalStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class SubmitTimeAPITests {

    lateinit var au : FakeAuthenticationUtilities
    lateinit var tru : FakeTimeRecordingUtilities
    private val defaultStartDate = "2021-01-01"

    @Before
    fun init() {
        au = FakeAuthenticationUtilities()
        tru = FakeTimeRecordingUtilities()
    }

    // region role tests

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_RegularUser() {
        val sd = makeSdForSubmit(user = DEFAULT_REGULAR_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_ApproverUser() {
        val sd = makeSdForSubmit(user = DEFAULT_APPROVER_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_AdminUser() {
        val sd = makeSdForSubmit(user = DEFAULT_ADMIN_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    @Category(APITestCategory::class)
    @Test
    fun testSubmittingTime_SystemUser() {
        val sd = makeSdForSubmit(user = SYSTEM_USER)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.FORBIDDEN, response)
    }

    // endregion

    @Test
    fun testSubmittingTime_InvalidStartDate() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "The date for submitting time was not interpreted as a date. You sent \"a1\".  Format is YYYY-MM-DD",
            false,
            ViewTimeAPI.path
        )
        val sd = makeSdForSubmit(startDate = "a1")

        // the API processes the client input
        val result = SubmitTimeAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If you pass in [r3z.timerecording.api.SubmitTimeAPI.Elements.UNSUBMIT] set to "true",
     * it will unsubmit your time
     */
    @Test
    fun testUnsubmittingTime() {
        val sd = makeSdForSubmit(unsubmit = true)

        // the API processes the client input
        val response = SubmitTimeAPI.handlePost(sd).statusCode
        assertEquals(StatusCode.SEE_OTHER, response)
    }

    /**
     * If you try to unsubmit time that is already approved,
     * it will fail
     */
    @Test
    fun testUnsubmittingApprovedTime() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "This time period is approved.  Cannot operate on approved time periods.",
            false,
            ViewTimeAPI.path
        )
        tru.isApprovedBehavior = { ApprovalStatus.APPROVED }
        val sd = makeSdForSubmit(unsubmit = true)

        // the API processes the client input
        val result = SubmitTimeAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    /**
     * If you try to submit time that is already submitted,
     * it will fail
     */
    @Test
    fun testSubmittingAlreadySubmitted() {
        val expected = MessageAPI.createCustomMessageRedirect(
            "This time period is already submitted.  Cannot submit on this period again.",
            false,
            ViewTimeAPI.path
        )
        tru.isInASubmittedPeriodBehavior = { true }
        val sd = makeSdForSubmit()

        // the API processes the client input
        val result = SubmitTimeAPI.handlePost(sd)

        assertEquals(expected, result)
    }

    @Test
    fun testSubmitting_MissingRequired_StartDate() {
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.UNSUBMIT.getElemName() to "true",
            )
        )
        val sd =  makeSdForSubmit(data, user = DEFAULT_ADMIN_USER)

        val ex = assertThrows(InexactInputsException::class.java) { SubmitTimeAPI.handlePost(sd) }

        assertEquals("expected keys: [start_date, unsubmit]. received keys: [unsubmit]", ex.message)
    }

    @Test
    fun testSubmitting_MissingRequired_Unsubmit() {
        val data = PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to DEFAULT_DATE.stringValue,
            )
        )
        val sd =  makeSdForSubmit(data, user = DEFAULT_ADMIN_USER)

        val ex = assertThrows(InexactInputsException::class.java) { SubmitTimeAPI.handlePost(sd) }

        assertEquals("expected keys: [start_date, unsubmit]. received keys: [start_date]", ex.message)
    }


    /**
     * A test helper for this class, just to remove repetitive boilerplate
     */
    private fun makeSdForSubmit(data: PostBodyData? = null, user: User = DEFAULT_REGULAR_USER, startDate: String = defaultStartDate, unsubmit: Boolean = false): ServerData {
        val bodyData = data ?: PostBodyData(
            mapOf(
                SubmitTimeAPI.Elements.START_DATE.getElemName() to startDate,
                SubmitTimeAPI.Elements.UNSUBMIT.getElemName() to unsubmit.toString(),
            )
        )
        return makeServerData(bodyData, tru, au, user = user, path = SubmitTimeAPI.path)
    }

}