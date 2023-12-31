package r3z.authentication.utility

import r3z.authentication.types.*
import r3z.system.misc.DEFAULT_EMPLOYEE
import r3z.system.misc.DEFAULT_INVITATION
import r3z.system.misc.DEFAULT_USER
import r3z.system.misc.types.DateTime
import r3z.timerecording.types.Employee

/**
 * Used as a mock object for testing
 */
class FakeAuthenticationUtilities (
    var registerBehavior : () -> RegistrationResult = { RegistrationResult(RegistrationResultStatus.SUCCESS, DEFAULT_USER) },
    var registerWithEmployeeBehavior : () -> RegistrationResult = { RegistrationResult(RegistrationResultStatus.SUCCESS, DEFAULT_USER) },
    var loginBehavior : () -> Pair<LoginResult, User> = {Pair(LoginResult.SUCCESS, SYSTEM_USER)},
    var getUserForSessionBehavior: () -> User = { NO_USER },
    var createNewSessionBehavior: () -> String = {""},
    var logoutBehavior: () -> Unit = {},
    var addRoleToUserBehavior: () -> User = { SYSTEM_USER },
    var createInvitationBehavior: () -> Invitation = { DEFAULT_INVITATION },
    var getEmployeeFromInvitationCodeBehavior: () -> Employee = { DEFAULT_EMPLOYEE },
    var removeInvitationBehavior: () -> Boolean = { true },
    var listAllInvitationsBehavior: () -> Set<Invitation> = { setOf() },
    var changePasswordBehavior: () -> ChangePasswordResult = { ChangePasswordResult.SUCCESSFULLY_CHANGED },
    var getUserByEmployeeBehavior: () -> User = { DEFAULT_USER },
    var listUsersByRoleBehavior: () -> Set<User> = { setOf(DEFAULT_USER) },
    ) : IAuthenticationUtilities {

    override fun register(username: UserName, password: Password, invitationCode: InvitationCode): RegistrationResult {
       return registerBehavior()
    }

    override fun registerWithEmployee(username: UserName, password: Password, employee: Employee): RegistrationResult {
       return registerWithEmployeeBehavior()
    }

    override fun login(username: UserName, password: Password): Pair<LoginResult, User> {
        return loginBehavior()
    }

    override fun getUserForSession(sessionToken: String): User {
        return getUserForSessionBehavior()
    }

    override fun createNewSession(user: User, time: DateTime, rand: () -> String): String {
        return createNewSessionBehavior()
    }

    override fun logout(user: User) {
        logoutBehavior()
    }

    override fun addRoleToUser(user: User, role: Role): User {
        return addRoleToUserBehavior()
    }

    override fun createInvitation(employee: Employee, datetime: DateTime, randomCode: () -> String): Invitation {
        return createInvitationBehavior()
    }

    override fun getEmployeeFromInvitationCode(invitationCode: InvitationCode): Employee {
        return getEmployeeFromInvitationCodeBehavior()
    }

    override fun removeInvitation(employee: Employee): Boolean {
        return removeInvitationBehavior()
    }

    override fun listAllInvitations(): Set<Invitation> {
        return listAllInvitationsBehavior()
    }

    override fun changePassword(user: User, password: Password): ChangePasswordResult {
        return changePasswordBehavior()
    }

    override fun getUserByEmployee(employee: Employee): User {
        return getUserByEmployeeBehavior()
    }

    override fun listUsersByRole(role: Role): Set<User> {
        return listUsersByRoleBehavior()
    }

}