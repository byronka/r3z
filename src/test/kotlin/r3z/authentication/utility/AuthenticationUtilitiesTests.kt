package r3z.authentication.utility

import r3z.authentication.exceptions.UnpermittedOperationException
import r3z.authentication.types.*
import r3z.persistence.types.DataAccess
import r3z.persistence.utility.PureMemoryDatabase
import r3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import r3z.system.config.LENGTH_OF_BYTES_OF_SESSION_STRING
import r3z.system.misc.*
import r3z.system.misc.utility.getTime
import r3z.timerecording.types.NO_EMPLOYEE
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(IntegrationTestCategory::class)
class AuthenticationUtilitiesTests {

    private lateinit var authUtils : AuthenticationUtilities
    private lateinit var pmd: PureMemoryDatabase
    private lateinit var cu : CurrentUser
    private lateinit var userDataAccess: DataAccess<User>
    private lateinit var sessionDataAccess: DataAccess<Session>
    private lateinit var invitationDataAccess: DataAccess<Invitation>

    @Before
    fun init() {
        cu = CurrentUser(DEFAULT_ADMIN_USER)
        pmd = createEmptyDatabase()
        userDataAccess = pmd.dataAccess(User.directoryName)
        sessionDataAccess = pmd.dataAccess(Session.directoryName)
        invitationDataAccess = pmd.dataAccess(Invitation.directoryName)
        authUtils = AuthenticationUtilities(pmd, testLogger, CurrentUser(SYSTEM_USER))
    }

    @Test
    fun `It should not be possible to register a new user with an empty password`() {
        val ex = assertThrows(IllegalArgumentException::class.java){Password("")}

        assertEquals(passwordMustNotBeBlankMsg, ex.message)
    }

    /****************
     * Length Cases *
     ****************/

    /**
     * At a certain point, a password can be too long.
     */
    @Test
    fun `It should not be possible to create a password longer than maxPasswordSize characters`() {
        val ex = assertThrows(IllegalArgumentException::class.java){Password("a".repeat(maxPasswordSize+1))}
        assertEquals(passwordMustNotBeTooLargeMsg, ex.message)
    }

    /**
     * At a certain point, a username can be too long.
     */
    @Test
    fun `It should not be possible to create a username longer than maxUserNameSize characters`() {
        val ex = assertThrows(IllegalArgumentException::class.java){ UserName("a".repeat(maxUserNameSize+1)) }
        assertEquals(tooLargeUsernameMsg, ex.message)
    }

    @Test
    fun `A maxPasswordSize-character password should succeed`() {
        val password = Password("a".repeat(maxPasswordSize))
        assertEquals(password.value, "a".repeat(maxPasswordSize))
    }


    @Test
    fun `A maxUserNameSize-character username should succeed`() {
        val username = UserName("a".repeat(maxUserNameSize))

        assertEquals(username.value, "a".repeat(maxUserNameSize))
    }

    /**
     * At a certain point, a password can be too short. Under 12 is probably abysmal.
     */
    @Test
    fun `A less-than-minPasswordSize character password should fail`() {
        val ex = assertThrows(IllegalArgumentException::class.java){Password("a".repeat(minPasswordSize-1))}
        assertEquals(passwordMustBeLargeEnoughMsg, ex.message)
    }

    /**
     * For sanity's sake, let's say that 2 characters is too short for a username
     */
    @Test
    fun `A less-than-minUserNameSize username should be considered too short`() {
        val ex = assertThrows(IllegalArgumentException::class.java){ UserName("a".repeat(minUserNameSize-1)) }
        assertEquals(tooSmallUsernameMsg, ex.message)
    }

    @Test
    fun `An empty username should be indicated as such`() {
        val ex = assertThrows(IllegalArgumentException::class.java){ UserName("") }
        assertEquals(usernameCannotBeEmptyMsg, ex.message)
    }

    /**
     * Three-character usernames would be ok - maybe initials
     */
    @Test
    fun `A minUserNameSize-character username should be considered ok`() {
        val minSizedUsername = "a".repeat(minUserNameSize)
        val username = UserName(minSizedUsername)

        assertEquals(username.value, minSizedUsername)
    }

    @Test
    fun `A minPasswordSize character password is a-ok`() {
        val password = Password("a".repeat(minPasswordSize))

        assertEquals(password.value, "a".repeat(minPasswordSize))
    }

    @Test
    fun `A password greater than minPasswordSize chars should pass`() {
        val password = Password("a".repeat(minPasswordSize+1))

        assertEquals(password.value, "a".repeat(minPasswordSize+1))
    }

    @Test
    fun `An account should not be created if the user already exists`() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }

        val result = authUtils.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE)

        assertEquals(RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, result.status)
    }

    @Test
    fun `should create a cryptographically secure hash from a password`() {
        assertEquals(DEFAULT_HASH_STRING, DEFAULT_HASH.value)
    }

    @Test
    fun `two different passwords should create different hashes`() {
        val result2 = Hash.createHash(Password(DEFAULT_PASSWORD.value + "a"), DEFAULT_SALT)
        assertNotEquals(DEFAULT_HASH, result2)
    }

    @Test
    fun `two large different passwords should create different hashes`() {
        val result = Hash.createHash(Password("a".repeat(200)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(201)), DEFAULT_SALT)
        assertNotEquals(result, result2)
    }

    @Test
    fun `two large equal passwords should create equal hashes`() {
        val result = Hash.createHash(Password("a".repeat(200)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(200)), DEFAULT_SALT)
        assertEquals(result, result2)
    }

    @Test
    fun `two small different passwords should create different hashes`() {
        val result = Hash.createHash(Password("a".repeat(12)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(13)), DEFAULT_SALT)
        assertNotEquals(result, result2)
    }

    @Test
    fun `two small equal passwords should create equal hashes`() {
        val result = Hash.createHash(Password("a".repeat(12)), DEFAULT_SALT)
        val result2 = Hash.createHash(Password("a".repeat(12)), DEFAULT_SALT)
        assertEquals(result, result2)
    }

    @Test
    fun `two small different passwords with small salts should create different hashes`() {
        val result = Hash.createHash(Password("a".repeat(12)), Salt("b"))
        val result2 = Hash.createHash(Password("a".repeat(13)), Salt("b"))
        assertNotEquals(result, result2)
    }

    /**
     * Contrary to my typical sensibilities, this algorithm we're using for hashing,
     * (see [Hash.createHash]), goes slowly for security reasons.  According to my
     * research, by requiring a slow algorithm, an attacker would be very slowed down
     * in their attempt to brute force their way in.
     *
     * In this test we are documenting that slowness.
     */
    @Test
    fun `test I guess this hash algorithm running slowly is part of the appeal`() {
        val maxMillisAllowed = 1000
        val (time, _) = getTime{
            repeat(5) {
                DEFAULT_HASH
            }
        }
        assertTrue("We would like to see this run in less than $maxMillisAllowed millis, it took $time", time < maxMillisAllowed)
    }

    @Test
    fun `Should throw exception if we pass in an empty string`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered(UserName("")) }
        assertEquals(usernameCannotBeEmptyMsg, thrown.message)
    }

    @Test
    fun `Should throw exception if we pass in all whitespace`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) { authUtils.isUserRegistered(UserName("   ")) }
        assertEquals(usernameCannotBeEmptyMsg, thrown.message)
    }

    /**
     * Cursory tests to work out the functionality of getSalt
     */
    @Test
    fun `two salts should give differeing hashes`() {
        val first = DEFAULT_HASH
        val salt = Hash.getSalt()
        val second = Hash.createHash(DEFAULT_PASSWORD, salt)
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
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        assertEquals(LoginResult.SUCCESS, status)
    }

    /**
     * I should get a failure status if I log in with the wrong password
     */
    @Test
    fun `should get failure with wrong password`() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        val (status, _) = authUtils.login(DEFAULT_USER.name, Password("wrongwrongwrong"))
        assertEquals(LoginResult.FAILURE, status)
    }


    /**
     * I should get an error telling me my user doesn't exist if I log in with an unregistered user
     */
    @Test
    fun `should get descriptive failure with nonreal user`() {
        val (status, _) = authUtils.login(DEFAULT_USER.name, Password("arbitraryarbitrary"))
        assertEquals(LoginResult.NOT_REGISTERED, status)
    }

    /**
     * We will give the user a token - a string - they can use to
     * instantly confirm they are authenticated with us.
     *
     * They hold this value in the cookie we hand them.
     */
    @Test
    fun testShouldConfirmUserAuthenticationBySessionToken() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        sessionDataAccess.actOn { s -> s.add(Session(1, DEFAULT_SESSION_TOKEN, DEFAULT_USER, DEFAULT_DATETIME)) }
        assertEquals(DEFAULT_USER, authUtils.getUserForSession(DEFAULT_SESSION_TOKEN))
    }

    /**
     * Creating a session is just a matter of storing a new
     * entry in the sessions data structure, with the session
     * identifier - a randomly generated string - and the user
     * who is considered authenticated.
     */
    @Test
    fun testShouldCreateNewSession() {
        val newSessionId = authUtils.createNewSession(DEFAULT_USER)
        assertEquals(LENGTH_OF_BYTES_OF_SESSION_STRING, newSessionId.length)
    }

    /**
     * If somehow the user were to able to attempt to logout
     * while already logged out, an exception should be thrown
     */
    @Test
    fun testShouldFailDeletingSessionsIfAlreadyLoggedOut() {
        val pmd = createEmptyDatabase()
        val au = AuthenticationUtilities(
            pmd,
            testLogger,
            CurrentUser(DEFAULT_ADMIN_USER),
        )

        val ex = assertThrows(IllegalStateException::class.java) { au.logout(DEFAULT_USER) }
        assertEquals("There must exist a session in the database for (${DEFAULT_USER.name.value}) in order to delete it", ex.message)
    }

    /**
     * Tests that an admin can add an admin role to a user
     */
    @Test
    fun testAdminShouldAddAdminRoleToUser() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        val elevatedUser = authUtils.addRoleToUser(DEFAULT_USER, Role.ADMIN)
        assertEquals(Role.ADMIN, elevatedUser.role)
    }

    /**
     * Tests that an admin can add an approver role to a user
     */
    @Test
    fun testAdminShouldAddApproverRoleToUser() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        val elevatedUser = authUtils.addRoleToUser(DEFAULT_USER, Role.APPROVER)
        assertEquals(Role.APPROVER, elevatedUser.role)
    }

    /**
     * Tests that a non-admin cannot add an approver role to a user
     */
    @Test
    fun testRegularUserShouldFailToAddApproverRoleToUser() {
        val au = AuthenticationUtilities(pmd, testLogger, CurrentUser(DEFAULT_REGULAR_USER))
        assertThrows(UnpermittedOperationException::class.java) { au.addRoleToUser(DEFAULT_USER, Role.APPROVER) }
    }

    /**
     * See [Invitation]
     */
    @Test
    fun testCanCreateInvitation() {
        val result = authUtils.createInvitation(DEFAULT_EMPLOYEE, DEFAULT_DATETIME) { "abc123" }
        assertEquals(DEFAULT_INVITATION, result)
    }

    @Test
    fun testCanRemoveInvitation() {
        invitationDataAccess.actOn { i -> i.add(DEFAULT_INVITATION) }
        val result = authUtils.removeInvitation(DEFAULT_EMPLOYEE)
        assertTrue(result)
    }

    @Test
    fun testCanChangePassword() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        val result = authUtils.changePassword(DEFAULT_USER, Password("newPasswordHere"))
        assertEquals(ChangePasswordResult.SUCCESSFULLY_CHANGED, result)
    }

    /**
     * If we pass in [NO_EMPLOYEE] for this method,
     * we'll get back [NO_USER]
     */
    @Test
    fun testGetUserByEmployee_NoEmployee() {
        val result = authUtils.getUserByEmployee(NO_EMPLOYEE)
        assertEquals(NO_USER, result)
    }

    /**
     * If we pass in an employee value that doesn't
     * match any user in the database,
     * we'll get back [NO_USER]
     */
    @Test
    fun testGetUserByEmployee_NoEmployeeFound() {
        val result = authUtils.getUserByEmployee(DEFAULT_EMPLOYEE)
        assertEquals(NO_USER, result)
    }

    /**
     * happy path - we get the user for an employee
     */
    @Test
    fun testGetUserByEmployee() {
        userDataAccess.actOn { u -> u.add(DEFAULT_USER) }
        val result = authUtils.getUserByEmployee(DEFAULT_EMPLOYEE)
        assertEquals(DEFAULT_USER, result)
    }

    @Test
    fun `it should be possible to obtain a list of users by their role`() {
        userDataAccess.actOn { u -> u.add(DEFAULT_REGULAR_USER) }
        val result = authUtils.listUsersByRole(Role.REGULAR)
        assertEquals(result, setOf(DEFAULT_REGULAR_USER))
    }

    @Test
    fun `I should get an empty list if I search for a role that has no one in it`() {
        // add an admin role
        userDataAccess.actOn { u -> u.add(DEFAULT_ADMIN_USER) }
        // search for regular role
        val result = authUtils.listUsersByRole(Role.REGULAR)
        assertEquals(result, emptySet<User>())
    }


}