package r3z.timerecording.api

import r3z.authentication.types.Role
import r3z.server.types.Element
import r3z.server.types.PostEndpoint
import r3z.server.types.PreparedResponseData
import r3z.server.types.ServerData
import r3z.server.utility.AuthUtilities.Companion.doPOSTAuthenticated
import r3z.server.utility.ServerUtilities.Companion.redirectTo
import r3z.timerecording.types.NO_TIMEENTRY
import r3z.timerecording.types.TimeEntryId

class DeleteTimeAPI {

    companion object: PostEndpoint {

        override fun handlePost(sd: ServerData): PreparedResponseData {
            return doPOSTAuthenticated(
                sd,
                requiredInputs,
                ViewTimeAPI.path,
                Role.SYSTEM, Role.ADMIN, Role.APPROVER, Role.REGULAR) {
                    val timeEntryId = TimeEntryId.make(sd.ahd.data.mapping[ViewTimeAPI.Elements.ID_INPUT.getElemName()])
                    val timeEntry = sd.bc.tru.findTimeEntryById(timeEntryId)
                    check(timeEntry != NO_TIMEENTRY) { "No time entry found with that id" }
                    sd.bc.tru.deleteTimeEntry(timeEntry)

                    val currentPeriod = sd.ahd.data.mapping[ViewTimeAPI.Elements.TIME_PERIOD.getElemName()]
                    val viewEntriesPage = ViewTimeAPI.path + if (currentPeriod.isNullOrBlank()) "" else "" + "?" + ViewTimeAPI.Elements.TIME_PERIOD.getElemName() + "=" + currentPeriod
                    redirectTo(viewEntriesPage)
            }

        }

        override val requiredInputs: Set<Element>
            get() = setOf(ViewTimeAPI.Elements.ID_INPUT)

        override val path: String
            get() = "deletetime"

    }
}