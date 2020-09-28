package coverosR3z.authentication

import coverosR3z.createTimeEntryPreDatabase
import coverosR3z.domainobjects.*
import coverosR3z.domainobjects.LoginStatuses.*
import coverosR3z.timerecording.FakeTimeEntryPersistence
import coverosR3z.timerecording.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException

class AuthenticationUtilitiesTests {
    lateinit var authUtils : AuthenticationUtilities
    lateinit var ap : FakeAuthPersistence

    @Before
    fun init() {
        ap = FakeAuthPersistence()
        authUtils = AuthenticationUtilities(ap, FakeCurrentUserAccessor())
    }

    @Test
    fun `It should not be possible to register a new user with an empty password`() {
        val result = authUtils.register("matt", "")

        assertEquals("the result should clearly indicate an empty password", RegistrationResult.EMPTY_PASSWORD, result)
    }

    /****************
     * Length Cases *
     ****************/

    /**
     * At a certain point, a password can be too long.
     */
    @Test
    fun `It should not be possible to create a password longer than 255 characters`() {
        val password = "a".repeat(256)
        assert(password.length == 256)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.PASSWORD_TOO_LONG, result)
    }

    @Test
    fun `A 255-character password should succeed`() {
        val password = "a".repeat(255)
        assert(password.length == 255)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    /**
     * At a certain point, a password can be too short. Under 12 is probably abysmal.
     */
    @Test
    fun `A 11 character password should fail`() {
        val password = "a".repeat(11)
        assert(password.length == 11)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.PASSWORD_TOO_SHORT, result)
    }

    @Test
    fun `An 12 character password is a-ok`() {
        val password = "a".repeat(12)
        assert(password.length == 12)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    @Test
    fun `A password greater than 12 chars should pass`() {
        val password = "a".repeat(13)

        val result = authUtils.register("matt", password)

        assertEquals(RegistrationResult.SUCCESS, result)
    }

    @Test
    fun `An account should not be created if the user already exists`() {
        ap.isUserRegisteredBehavior = {true}

        val result = authUtils.register("matt", "just don't care")

        assertEquals(RegistrationResult.ALREADY_REGISTERED, result)
    }

    @Test
    fun `Should determine if a particular username is for a registered user`() {
        ap = FakeAuthPersistence(isUserRegisteredBehavior = {true})
        authUtils = AuthenticationUtilities(ap, FakeCurrentUserAccessor())

        val result = authUtils.isUserRegistered("jenna")
        assertEquals(true, result)
    }

    /**
     * Say we have "password123", we should get what we know unsalted sha-256 hashes that as
     */
    @Test
    fun `should create a cryptographically secure hash from a password`() {
        val password = "password123"
        val result = Hash.createHash(password)
        assertEquals("ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f", result.value)
    }

    @Test
    fun `Should throw exception if we pass in an empty string`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered("") }
        assertEquals("no username was provided to check", thrown.message)
    }

    @Test
    fun `Should throw exception if we pass in all whitespace`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered("   ") }
        assertEquals("no username was provided to check", thrown.message)
    }

    /**
     * Cursory tests to work out the functionality of getSalt
     */
    @Test
    fun `password hash should be salted`() {
        val first = Hash.createHash("password123")
        val salt : String = Hash.getSalt()
        val second = Hash.createHash("password123" + salt)
        assertNotEquals(first, second)
    }

    /**
     *The intention of this is to ensure that the getSalt method is implemented in such a way
     * that it provides randomness. Nothing in this test actually ensures secure randomness.
     */
    @Test
    fun `salts should not be the same each time`(){
        val first = Hash.getSalt()
        val second = Hash.getSalt()
        assertNotEquals(first, second)
    }

    /**
     * I should get a success status if I log in with valid credentials
     */
    @Test
    fun `should get success with valid login`() {
        val salt = Hash.getSalt()
        val wellSeasoned = "password123$salt"
        val fap = FakeAuthPersistence(
                getUserBehavior= { User(1, "matt", Hash.createHash(wellSeasoned), salt, null) }
        )
        val au = AuthenticationUtilities(fap, FakeCurrentUserAccessor())
        val (status, _) = au.login("matt", "password123")
        assertEquals(SUCCESS, status)
    }

    /**
     * I should get a failure status if I log in with the wrong password
     */
    @Test
    fun `should get failure with wrong password`() {

        val salt = Hash.getSalt()
        val wellSeasoned = "password123$salt"
        val fap = FakeAuthPersistence(
                getUserBehavior= { User(1, "matt", Hash.createHash(wellSeasoned), salt, null) }
        )
        val au = AuthenticationUtilities(fap, FakeCurrentUserAccessor())
        val (status, _) = au.login("matt", "wrong")
        assertEquals(FAILURE, status)
    }


    /**
     * I should get an error telling me my user doesn't exist if I log in with an unregistered user
     */
    @Test
    fun `should get descriptive failure with nonreal user`() {
        val fap = FakeAuthPersistence(
                getUserBehavior= { null }
        )
        val au = AuthenticationUtilities(fap, FakeCurrentUserAccessor())
        val (status, _) = au.login("matt", "arbitrary")
        assertEquals(NOT_REGISTERED, status)
    }

    /**
     * Here, we want to obtain the universally-accessible
     * information of who is running commands.  The current user.
     */
    @Test
    fun `should be able to get the current user`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()
        val user = User(1, "matt", Hash.createHash(""), "", null)
        cua.set(user)

        val currentUser = cua.get()

        assertEquals(user, currentUser)
    }

    @Test
    fun `should store who the user is during login process`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()

        val username = "mitch"
        val password = "password12345"
        val userId = 1
        val salt = "abc123"
        val user = User(userId, username, Hash.createHash("$password$salt"), salt, null)
        val fap = FakeAuthPersistence(
                getUserBehavior= { user }
        )
        val au = AuthenticationUtilities(fap, cua)

        au.login(username, password)

        assertEquals(user, cua.get())
    }

    @Test
    fun `When no user is logged in, time entry should throw an exception`() {
        val cua = CurrentUserAccessor()
        cua.clearCurrentUserTestOnly()

        val trt = TimeRecordingUtilities(FakeTimeEntryPersistence(), cua)

        assertThrows(AssertionError::class.java) {trt.recordTime(createTimeEntryPreDatabase())}
    }

}