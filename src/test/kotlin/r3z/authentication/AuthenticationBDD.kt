package r3z.authentication

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.RegistrationResultStatus
import r3z.authentication.types.SYSTEM_USER
import r3z.authentication.types.User
import r3z.authentication.utility.AuthenticationUtilities
import r3z.authentication.utility.FakeAuthenticationUtilities
import r3z.bddframework.BDD
import r3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import r3z.system.misc.*
import r3z.timerecording.FakeTimeRecordingUtilities
import r3z.timerecording.types.Employee
import r3z.timerecording.types.RecordTimeResult
import r3z.timerecording.types.StatusEnum
import r3z.timerecording.utility.ITimeRecordingUtilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationBDD {

    @BDD
    @Test
    fun `I cannot change someone else's time`() {
        val s = AuthenticationUserStory.getScenario("I cannot change someone else's time")

        val (tru, sarah) = initializeTwoUsersAndLogin()
        s.markDone("Given I am logged in as user alice and employees Sarah and Alice exist in the database,")

        val preparedEntry = createTimeEntryPreDatabase(sarah)
        val result = tru.createTimeEntry(preparedEntry)
        s.markDone("when I try to add a time-entry for Sarah,")

        assertEquals(RecordTimeResult(StatusEnum.USER_EMPLOYEE_MISMATCH, null), result)
        s.markDone("then the system disallows it.")
    }

    @BDD
    @Test
    fun `I should not be able to register a user if they are already registered`() {
        val s = AuthenticationUserStory.getScenario("I should not be able to register a user if they are already registered")

        val au = setupPreviousRegisteredUser()
        s.markDone("Given I have previously been registered,")

        val result = au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE)
        s.markDone("when I try to register again,")

        assertEquals("The user shouldn't be allowed to register again",
            RegistrationResultStatus.USERNAME_ALREADY_REGISTERED, result.status)
        s.markDone("then the system records that the registration failed.")
    }

    @BDD
    @Test
    fun `I should be able to log in once I'm a registered user`() {
        val s = AuthenticationUserStory.getScenario("I should be able to log in once I'm a registered user")

        val au = setupPreviousRegistration()
        s.markDone("Given I have registered,")

        val (_, resultantUser) = au.login(DEFAULT_USER.name, DEFAULT_PASSWORD)
        s.markDone("when I enter valid credentials,")

        assertSystemRecognizesUser(au, resultantUser)
        s.markDone("then the system knows who I am.")
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    companion object {

        val au : FakeAuthenticationUtilities = FakeAuthenticationUtilities()
        val tru : FakeTimeRecordingUtilities = FakeTimeRecordingUtilities()

        private fun initializeTwoUsersAndLogin(): Pair<ITimeRecordingUtilities, Employee> {
            val (tru, _, sarah) = initializeAUserAndLogin()
            return Pair(tru, sarah)
        }

    }

    private fun assertSystemRecognizesUser(
        au: AuthenticationUtilities,
        resultantUser: User
    ) {
        assertTrue(au.isUserRegistered(resultantUser.name))
    }

    private fun setupPreviousRegistration(): AuthenticationUtilities {
        val pmd = createEmptyDatabase()
        val au = AuthenticationUtilities(pmd, testLogger, CurrentUser(SYSTEM_USER))
        au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE)
        return au
    }

    private fun setupPreviousRegisteredUser(): AuthenticationUtilities {
        val pmd = createEmptyDatabase()
        val au = AuthenticationUtilities(pmd, testLogger, CurrentUser(SYSTEM_USER))
        au.registerWithEmployee(DEFAULT_USER.name, DEFAULT_PASSWORD, DEFAULT_EMPLOYEE)
        return au
    }


}
