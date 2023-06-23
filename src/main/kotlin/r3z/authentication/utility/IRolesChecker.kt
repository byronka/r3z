package r3z.authentication.utility

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.Role

interface IRolesChecker {

    fun checkAllowed(cu: CurrentUser, vararg roles: Role)
}
