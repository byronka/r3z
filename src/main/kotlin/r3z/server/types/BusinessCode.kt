package r3z.server.types

import r3z.authentication.utility.IAuthenticationUtilities
import r3z.timerecording.utility.ITimeRecordingUtilities

/**
 * A wrapper for the business-related objects
 */
data class BusinessCode(val tru: ITimeRecordingUtilities, val au: IAuthenticationUtilities)