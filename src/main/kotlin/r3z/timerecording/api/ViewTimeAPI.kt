package r3z.timerecording.api

import r3z.authentication.exceptions.UnpermittedOperationException
import r3z.authentication.types.Role
import r3z.system.misc.types.Date
import r3z.system.misc.utility.checkParseToInt
import r3z.system.misc.utility.safeAttr
import r3z.system.misc.utility.safeHtml
import r3z.server.types.*
import r3z.server.utility.AuthUtilities.Companion.doGETRequireAuth
import r3z.server.utility.PageComponents
import r3z.timerecording.types.*
import r3z.timerecording.utility.ITimeRecordingUtilities
import java.util.*

class ViewTimeAPI {

    enum class Elements (private val value: String = "") : Element {
        // edit fields
        PROJECT_INPUT_EDIT("edit-project-entry"),
        DATE_INPUT_EDIT("edit-date"),
        TIME_INPUT_EDIT("edit-time"),
        DETAILS_INPUT_EDIT("edit-details"),

        /**
         * This is used to indicate a time entry row that
         * is in the midst of being edited, and is also
         * sent to the enter time api to indicate we
         * want an edit, not a new entry
         */
        BEING_EDITED("being_edited"),

        // create fields
        PROJECT_INPUT_CREATE("create-project-entry"),
        DATE_INPUT_CREATE("create-date"),
        TIME_INPUT_CREATE("create-time"),
        DETAILS_INPUT_CREATE("create-details"),

        // used for the name field, sent in POSTs
        PROJECT_INPUT("project_entry"),
        TIME_INPUT("time_entry"),
        DETAIL_INPUT("detail_entry"),
        DATE_INPUT("date_entry"),
        ID_INPUT("entry_id"),

        CREATE_TIME_ENTRY_FORM("enter_time_panel"),

        EDIT_BUTTON("editbutton"),

        /**
         * Used for creating a new time entry with a particular project
         * already selected
         */
        CANCEL_BUTTON("cancelbutton"),
        SAVE_BUTTON("savebutton"),
        DELETE_BUTTON("deletebutton"),
        CREATE_BUTTON("enter_time_button"),

        // query string items

        /**
         * a date which allows us to determine which time period to show
         */
        TIME_PERIOD("date"),

        /**
         * an employee id to allow choosing whose timesheet to show
         */
        REQUESTED_EMPLOYEE("emp"),

        /**
         * the id of a time entry we are editing
         */
        EDIT_ID("editid"),

        // navigation

        PREVIOUS_PERIOD("previous_period"),
        CURRENT_PERIOD("current_period"),
        NEXT_PERIOD("next_period"),

        // parts of the time entries

        SUBMIT_BUTTON("submitbutton"),

        /**
         * for approval of an employee's timesheet
         */
        EMPLOYEE_TO_APPROVE_INPUT("approval-employee"),
        APPROVAL_BUTTON("approval_button"),
        ;
        override fun getId(): String {
            return this.value
        }

        override fun getElemName(): String {
            return this.value
        }

        override fun getElemClass(): String {
            return this.value
        }
    }

    companion object : GetEndpoint {

        override fun handleGet(sd: ServerData): PreparedResponseData {
            return doGETRequireAuth(sd.ahd.user, Role.REGULAR, Role.APPROVER, Role.ADMIN) { renderTimeEntriesPage(sd) }
        }

        override val path: String
            get() = "timeentries"

        private fun projectsToOptions(projects: List<Project>): String {
            return projects.sortedBy { it.name.value }.joinToString("") {
                """<option value="${safeAttr(it.name.value)}" >"""
            }
        }


        /**
         * Top-level function for rendering everything needed for the time entries page
         */
        private fun renderTimeEntriesPage(sd: ServerData): String {
            // if we receive a query string like ?date=2020-06-12 we'll get
            // the time period it fits in
            val currentPeriod = obtainCurrentTimePeriod(sd)

            // let's see if they are asking for a particular employee's information
            val (employee, reviewingOtherTimesheet) = determineCriteriaForWhoseTimesheet(sd)

            val tru = sd.bc.tru

            val te = tru.getTimeEntriesForTimePeriod(employee, currentPeriod)
            val totalHours = Time(te.sumOf { it.time.numberOfMinutes }).getHoursAsString()
            val neededHours = Time(8 * 60 * TimePeriod.numberOfWeekdays(currentPeriod)).getHoursAsString()
            val editIdValue = sd.ahd.queryString[Elements.EDIT_ID.getElemName()]

            // either get the id as an integer or get null,
            // the code will handle either properly
            val idBeingEdited = determineWhichTimeEntryIsBeingEdited(editIdValue, reviewingOtherTimesheet)

            val projects = tru.listAllProjects()

            val approvalStatus = tru.isApproved(employee, currentPeriod.start)
            val (inASubmittedPeriod, submitButton) = processSubmitButton(
                employee,
                currentPeriod,
                reviewingOtherTimesheet,
                approvalStatus,
                sd
            )
            val switchEmployeeUI = createEmployeeSwitch(currentPeriod, sd)

            val approveUI = createApproveUI(
                reviewingOtherTimesheet,
                isSubmitted = inASubmittedPeriod,
                approvalStatus,
                employee,
                currentPeriod
            )
            val navMenu =
                createNavMenu(
                    submitButton,
                    switchEmployeeUI,
                    approveUI,
                    employee,
                    reviewingOtherTimesheet,
                    currentPeriod,
                    totalHours,
                    neededHours
                )

            val submittedString = if (inASubmittedPeriod) "Submitted" else "Unsubmitted"
            // show this if we are viewing someone else's timesheet
            val viewingHeader = if (! reviewingOtherTimesheet) "" else """<h2 id="viewing_whose_timesheet">Viewing ${safeHtml(employee.name.value)}'s <em>$submittedString</em> timesheet</h2>"""
            val timeEntryPanel = if (approvalStatus == ApprovalStatus.APPROVED) "" else renderTimeEntryPanel(
                te,
                idBeingEdited,
                projects,
                currentPeriod,
                inASubmittedPeriod,
                reviewingOtherTimesheet,
                sd
            )
            val hideEditButtons = inASubmittedPeriod || reviewingOtherTimesheet || approvalStatus == ApprovalStatus.APPROVED

            val dateToDailyHours = te
                .groupBy { it.date }
                .mapValues { (_,v) -> v.sumOf { it.time.numberOfMinutes } }
                .entries.joinToString(separator = ",") { "'${it.key.stringValue}' : ${it.value}" }

            val body = """
            <div id="outermost_container">
                <div id="inner_container">
                    <div id="control_surface">
                        $viewingHeader
                        $navMenu
                        $timeEntryPanel
                    </div>
                    <div id="timerows-container">
                        ${renderTimeRows(te, currentPeriod, hideEditButtons, idBeingEdited, tru, employee)}
                    </div>
                </div>    
            </div>
            <script>
                let timeentries = { $dateToDailyHours  }
            </script>
        """

            val viewingSelf = sd.ahd.user.employee == employee
            val title = if (viewingSelf) "Your time entries" else "${safeHtml(employee.name.value)}'s $submittedString timesheet "
            return PageComponents(sd).makeTemplate(title, "ViewTimeAPI", body,
                extraHeaderContent="""
                <link rel="stylesheet" href="viewtime.css" />
                <script src="viewtime.js"></script>
                """.trimIndent() )
        }

        private fun createApproveUI(reviewingOtherTimesheet: Boolean, isSubmitted: Boolean, approvalStatus: ApprovalStatus, employee: Employee, timePeriod: TimePeriod): String {
            if (! reviewingOtherTimesheet) return ""
            val renderDisabled = if (! isSubmitted) "disabled" else ""
            val isApproved = approvalStatus == ApprovalStatus.APPROVED
            val buttonHtml = if (! isApproved) {
                """<button id="${Elements.APPROVAL_BUTTON.getId()}" $renderDisabled>Approve</button>"""
            } else {
                """<button id="${Elements.APPROVAL_BUTTON.getId()}">Unapprove</button>"""
            }
            return """
            <form class="navitem" action="${ApproveApi.path}" method="post">
                <input type="hidden" name="${Elements.EMPLOYEE_TO_APPROVE_INPUT.getElemName()}" value="${employee.id.value}" />
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${timePeriod.start.stringValue}" />
                <input type="hidden" name="${ApproveApi.Elements.IS_UNAPPROVAL.getElemName()}" value="${isApproved.toString()
                .lowercase(Locale.getDefault())}" />
                $buttonHtml
            </form>
        """.trimIndent()
        }

        private fun obtainCurrentTimePeriod(sd: ServerData): TimePeriod {
            val dateQueryString: String? = sd.ahd.queryString[Elements.TIME_PERIOD.getElemName()]
            return calculateCurrentTimePeriod(dateQueryString)
        }

        /**
         * Creates the navigation menu for the timesheet entries.
         * There are several mechanisms at play here - what the admin
         * can see versus the approver versus the regular user.
         */
        private fun createNavMenu(
            submitButton: String,
            switchEmployeeUI: String,
            approveUI: String,
            employee: Employee,
            reviewingOtherTimesheet: Boolean,
            currentPeriod: TimePeriod,
            totalHours: String,
            neededHours: String,
        ): String {
            return """ 
                <nav id="control_panel">
                    $submitButton
                    $approveUI
                    $switchEmployeeUI
                    
                    <div id="time_period_selector">
                        ${currentPeriodButton(employee, reviewingOtherTimesheet)}
                        ${previousPeriodButton(currentPeriod, employee, reviewingOtherTimesheet)}
                        ${timeperiodDisplay(currentPeriod)}
                        ${totalHoursDisplay(totalHours)}
                        ${neededHoursDisplay(neededHours)}
                        ${nextPeriodButton(currentPeriod, employee, reviewingOtherTimesheet)}
                    </div>
                </nav>
                """.trimIndent()
        }

        private fun neededHoursDisplay(neededHours: String) =
            """       <div class="period_selector_item" id="needed_hours">
                                <label>need:</label>
                                <div id="needed_hours_value">$neededHours</div>
                            </div>"""

        private fun totalHoursDisplay(totalHours: String) = """ <div class="period_selector_item" id="total_hours">
                                <label>hours:</label>
                                <div id="total_hours_value">$totalHours</div>
                            </div>"""

        private fun timeperiodDisplay(currentPeriod: TimePeriod) =
            """     <div class="period_selector_item" id="timeperiod_display">
                                <div id="timeperiod_display_start">${currentPeriod.start.stringValue}</div>
                                <div id="timeperiod_display_end">${currentPeriod.end.stringValue}</div>
                            </div>"""

        /**
         * Creates the part of the UI that allows an admin or approver to switch
         * to seeing another person's timesheet
         */
        private fun createEmployeeSwitch(currentPeriod: TimePeriod, sd: ServerData): String {
            return if (sd.ahd.user.role !in listOf(Role.ADMIN, Role.APPROVER)) "" else """                
                <form id="employee_switch_form" class="navitem" action="$path">
                    <label id="view_other_timesheet_label">View other timesheet</label>
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                    <select id="employee-selector" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}">
                        <option selected value="">Self</option>
                        ${allEmployeesOptions(sd)}
                    </select>
                    <button>Switch</button>
                </form>
                """
        }

        private fun allEmployeesOptions(sd: ServerData): String {
            val employees = sd.bc.tru.listAllEmployees()
            return employees.filterNot { it == sd.ahd.user.employee }.joinToString(""){"""<option value="${it.id.value}">${it.name.value}</option>"""}
        }

        private fun nextPeriodButton(
            currentPeriod: TimePeriod,
            employee: Employee,
            reviewingOtherTimesheet: Boolean
        ): String {
            val employeeField = if (! reviewingOtherTimesheet) "" else
                """<input type="hidden" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}" value="${employee.id.value}" />"""
            return """           
            <form class="period_selector_item" action="$path">
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getNext().start.stringValue}" /> 
                $employeeField
                <button id="${Elements.NEXT_PERIOD.getId()}">❯</button>
            </form>
    """.trimIndent()
        }

        private fun previousPeriodButton(
            currentPeriod: TimePeriod,
            employee: Employee,
            reviewingOtherTimesheet: Boolean
        ): String {
            val employeeField = if (! reviewingOtherTimesheet) "" else
                """<input type="hidden" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}" value="${employee.id.value}" />"""
            return """      
            <form class="period_selector_item" action="$path">
                <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.getPrevious().start.stringValue}" /> 
                $employeeField
                <button id="${Elements.PREVIOUS_PERIOD.getId()}">❮</button>
            </form>
     """.trimIndent()
        }

        private fun currentPeriodButton(employee: Employee, reviewingOtherTimesheet: Boolean): String {
            val employeeField = if (! reviewingOtherTimesheet) "" else
                """<input type="hidden" name="${Elements.REQUESTED_EMPLOYEE.getElemName()}" value="${employee.id.value}" />"""
            return """       
            <form class="period_selector_item" action="$path">
                <button id="${Elements.CURRENT_PERIOD.getId()}">Current</button>
                $employeeField
            </form>
     """.trimIndent()
        }

        /**
         * A particular set of time entries may be submitted or non-submitted.
         * This goes through some of the calculations for that.
         */
        private fun processSubmitButton(
            employee: Employee,
            currentPeriod: TimePeriod,
            reviewingOtherTimesheet: Boolean,
            approvalStatus: ApprovalStatus,
            sd: ServerData,
        ): Pair<Boolean, String> {
            if (approvalStatus == ApprovalStatus.APPROVED) return Pair(true, "")

            // Figure out time period date from viewTimeAPITests
            val periodStartDate = currentPeriod.start
            val inASubmittedPeriod = sd.bc.tru.isInASubmittedPeriod(employee, periodStartDate)
            val submitButtonLabel = if (inASubmittedPeriod) "Unsubmit" else "Submit"
            val submitButton = if (reviewingOtherTimesheet) "" else """
    <form class="navitem" action="${SubmitTimeAPI.path}" method="post">
        <button id="${Elements.SUBMIT_BUTTON.getId()}">$submitButtonLabel</button>
        <input name="${SubmitTimeAPI.Elements.START_DATE.getElemName()}" type="hidden" value="${periodStartDate.stringValue}">
        <input name="${SubmitTimeAPI.Elements.UNSUBMIT.getElemName()}" type="hidden" value="$inASubmittedPeriod">
    </form>
    """.trimIndent()
            return Pair(inASubmittedPeriod, submitButton)
        }

        /**
         * We may receive an id of a time entry to edit.  This checks
         * whether we can render that.
         */
        private fun determineWhichTimeEntryIsBeingEdited(
            editidValue: String?,
            reviewingOtherTimesheet: Boolean
        ) = if (editidValue == null) {
            null
        } else {
            if (reviewingOtherTimesheet) {
                throw IllegalStateException(
                    "If you are viewing someone else's timesheet, " +
                            "you aren't allowed to edit any fields.  " +
                            "The ${Elements.EDIT_ID.getElemName()} key in the query string is not allowed."
                )
            }
            checkParseToInt(editidValue)
        }

        /**
         * We may have received a particular employee's id to determine which timesheet
         * to show.  This checks that
         */
        private fun determineCriteriaForWhoseTimesheet(sd: ServerData): Pair<Employee, Boolean> {
            val employeeQueryString: String? = sd.ahd.queryString[Elements.REQUESTED_EMPLOYEE.getElemName()]
            return if (! employeeQueryString.isNullOrBlank()) {
                if (sd.ahd.user.role == Role.REGULAR) {
                    throw UnpermittedOperationException("Your role does not allow viewing other employee's timesheets.  Your URL had a query string requesting to see a particular employee, using the key ${Elements.REQUESTED_EMPLOYEE.getElemName()}")
                }
                val id = EmployeeId.make(employeeQueryString)
                if (sd.ahd.user.employee.id == id) {
                    throw IllegalStateException("Error: makes no sense to request your own timesheet (employee id in query string was your own)")
                }
                val employee = sd.bc.tru.findEmployeeById(id)
                if (employee == NO_EMPLOYEE) {
                    throw java.lang.IllegalStateException("Error: employee id in query string (${id.value}) does not find any employee")
                }
                Pair(employee,true)
            } else {
                Pair(sd.ahd.user.employee, false)
            }
        }

        private fun calculateCurrentTimePeriod(dateQueryString: String?): TimePeriod {
            return if (dateQueryString != null) {
                val date = Date.make(dateQueryString)
                TimePeriod.getTimePeriodForDate(date)
            } else {
                TimePeriod.getTimePeriodForDate(Date.now())
            }
        }

        /**
         * Renders the panels for both time entry and editing time
         */
        private fun renderTimeEntryPanel(
            te: Set<TimeEntry>,
            idBeingEdited: Int?,
            projects: List<Project>,
            currentPeriod: TimePeriod,
            inASubmittedPeriod: Boolean,
            reviewingOtherTimesheet: Boolean,
            sd: ServerData
        ): String {
            return if (! (inASubmittedPeriod || reviewingOtherTimesheet)) {
                val dataEntryHtml = if (idBeingEdited != null) {
                    renderEditRow(te.single{it.id.value == idBeingEdited}, projects, currentPeriod)
                } else {
                    renderCreateTimeRow(currentPeriod, sd)
                }
                return """
                <div id="data-entry-container">
                    $dataEntryHtml
                </div>
            """.trimIndent()
            } else {
                ""
            }
        }

        private fun renderTimeRows(
            te: Set<TimeEntry>,
            currentPeriod: TimePeriod,
            hideEditButtons: Boolean,
            idBeingEdited: Int?,
            tru: ITimeRecordingUtilities,
            employee: Employee
        ): String {
            val timeEntriesByDate = te.groupBy { it.date }

            var readOnlyRows = ""
            val orderedTimeEntries = timeEntriesByDate.keys.sortedDescending()

            for (date in orderedTimeEntries) {
                readOnlyRows += renderTimeEntriesForDate(
                    timeEntriesByDate,
                    date,
                    currentPeriod,
                    hideEditButtons,
                    idBeingEdited,
                    tru,
                    employee
                )
            }

            return readOnlyRows
        }

        private fun renderTimeEntriesForDate(
            timeEntriesByDate: Map<Date, List<TimeEntry>>,
            date: Date,
            currentPeriod: TimePeriod,
            hideEditButtons: Boolean,
            idBeingEdited: Int?,
            tru: ITimeRecordingUtilities,
            employee: Employee
        ): String {
            val dailyHours = Time(timeEntriesByDate[date]?.sumOf { it.time.numberOfMinutes } ?: 0).getHoursAsString()
            val hoursThisWeek = tru.getTimeForWeek(employee,date).getHoursAsString()
            val dateHeaderString = "${date.viewTimeHeaderFormat}, Daily hours: $dailyHours, Weekly hours: $hoursThisWeek"

            val tableRows = timeEntriesByDate[date]
                ?.sortedBy { it.project.name.value }
                ?.joinToString("") {
                    renderReadOnlyRow(it, currentPeriod, hideEditButtons, idBeingEdited)
                }

            val actionColumnHeader = if (hideEditButtons) "" else """<th class="act"></th>"""

            return """
            
            <table>
                <caption><div>$dateHeaderString</div></caption>
                <thead>
                    <th class="prj">Project</th>
                    <th class="time">Time</th>
                    <th class="dtl">Details</th>
                    $actionColumnHeader
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>
        """.trimIndent()
        }

        private fun renderReadOnlyRow(
            it: TimeEntry,
            currentPeriod: TimePeriod,
            hideEditButtons: Boolean,
            idBeingEdited: Int?,
        ): String {

            val actContent = if (it.id.value == idBeingEdited) "" else """
            <a class="button" href="$path?${Elements.EDIT_ID.getElemName()}=${it.id.value}&${Elements.TIME_PERIOD.getElemName()}=${currentPeriod.start.stringValue}">edit</a>
        """.trimIndent()
            val actionColumn = if (hideEditButtons) "" else """
            <td>
                $actContent
            </td>
        """

            val isBeingEditedClass = if (it.id.value == idBeingEdited) """class="${Elements.BEING_EDITED.getElemClass()}"""" else ""

            val detailContent = if (it.details.value.isBlank()) "&nbsp;" else safeHtml(it.details.value)
            return """
        <tr $isBeingEditedClass id="time-entry-${it.id.value}">
            <td>
                ${safeHtml(it.project.name.value)}
            </td>
            <td>
                ${it.time.getHoursAsString()}
            </td>
            <td>
                $detailContent
            </td>
            $actionColumn
        </tr>
    """
        }

        /**
         * For entering new time
         */
        private fun renderCreateTimeRow(currentPeriod: TimePeriod, sd: ServerData): String {
            val projects = sd.bc.tru.listAllProjects()
            val defaultDateValue = if (currentPeriod == TimePeriod.getTimePeriodForDate(Date.now())) {
                Date.now()
            } else {
                currentPeriod.start
            }

            return """
            <form id="${Elements.CREATE_TIME_ENTRY_FORM.getId()}" action="${EnterTimeAPI.path}" method="post">
                <div class="row">
                    <div class="project">
                        <label for="${Elements.PROJECT_INPUT_CREATE.getId()}">Project:</label>
                        <input autofocus list="projects" type="text" placeholder="choose" id="${Elements.PROJECT_INPUT_CREATE.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required="required" />
                        <datalist id="projects">
                            ${projectsToOptions(projects)}
                        </datalist>
                    </div>
        
                    <div class="date">
                        <label for="${Elements.DATE_INPUT_CREATE.getId()}">Date:</label>
                        <select id="${Elements.DATE_INPUT_CREATE.getId()}" name="${Elements.DATE_INPUT.getElemName()}" required="required" >
                            ${createDateOptions(currentPeriod, defaultDateValue)}
                        </select>
                    </div>
                    
                    <div class="time">
                        <label for="${Elements.TIME_INPUT_CREATE.getId()}">Time:</label>
                        <input autocomplete="off" id="${Elements.TIME_INPUT_CREATE.getId()}" name="${Elements.TIME_INPUT.getElemName()}" type="number" inputmode="decimal" step="0.50" min="0" max="24" required="required" />
                    </div>
                </div>
                
                <div class="row">
                    <div class="details">
                        <label   for="${Elements.DETAILS_INPUT_CREATE.getId()}">Details:</label>
                        <textarea id="${Elements.DETAILS_INPUT_CREATE.getId()}" name="${Elements.DETAIL_INPUT.getElemName()}" maxlength="$MAX_DETAILS_LENGTH" ></textarea>
                    </div>
                    
                    <div class="action">
                        <button id="${Elements.CREATE_BUTTON.getId()}">Enter</button>
                    </div>
                </div>
            </form>
    """
        }

        /**
         * Creates the set of options for the date select
         */
        private fun createDateOptions(currentPeriod: TimePeriod, selectedDate: Date): String {

            return (currentPeriod.start.epochDay..currentPeriod.end.epochDay).joinToString (separator = "",
                transform = fun(it: Long): CharSequence {
                    return """<option ${if (it == selectedDate.epochDay) "selected" else ""} value="${Date(it).stringValue}">${Date(it).viewTimeHeaderFormat}</option>"""
                })
        }

        /**
         * Similar to [renderCreateTimeRow] but for editing entries
         */
        private fun renderEditRow(te: TimeEntry, projects: List<Project>, currentPeriod: TimePeriod): String {

            return """
                <form id="edit_time_panel" action="${EnterTimeAPI.path}" method="post">
                    <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                    <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                    <input type="hidden" name="${Elements.BEING_EDITED.getElemName()}" value="true" />
                    <div class="row">
                        <div class="project">
                            <label for="${Elements.PROJECT_INPUT_EDIT.getId()}">Project:</label>
                            <input autofocus list="projects" type="text" id="${Elements.PROJECT_INPUT_EDIT.getId()}" name="${Elements.PROJECT_INPUT.getElemName()}" required="required" value="${safeHtml(te.project.name.value)}" />
                            <datalist id="projects">
                                ${projectsToOptions(projects)}
                            </datalist>
                        </div>
                        
                        <div class="date">
                            <label for="${Elements.DATE_INPUT_EDIT.getId()}">Date:</label>
                            <select id="${Elements.DATE_INPUT_EDIT.getId()}" name="${Elements.DATE_INPUT.getElemName()}" required="required" >
                                ${createDateOptions(currentPeriod, te.date)}
                            </select>    
                        </div>
            
                        <div class="time">
                            <label for="${Elements.TIME_INPUT_EDIT.getId()}">Time:</label>
                            <script>let previoustime = ${te.time.numberOfMinutes}</script>
                            <input autocomplete="off" id="${Elements.TIME_INPUT_EDIT.getId()}" name="${Elements.TIME_INPUT.getElemName()}" 
                                type="number" inputmode="decimal" 
                                step="0.50" min="0" max="24" required="required"
                                value="${te.time.getHoursAsString()}" 
                                 />
                        </div>
                    </div>
                    
                    <div class="row">
                        <div class="details">
                            <label for="${Elements.DETAILS_INPUT_EDIT.getId()}">Details:</label>
                            <textarea id="${Elements.DETAILS_INPUT_EDIT.getId()}" name="${Elements.DETAIL_INPUT.getElemName()}" 
                                maxlength="$MAX_DETAILS_LENGTH">${safeHtml(te.details.value)}</textarea>
                        </div>
                    </div>

                    
                        </form>
                        <form id="cancellation_form" action="$path" method="get">
                            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                        </form>
                        <form id="delete_form" action="${DeleteTimeAPI.path}" method="post">
                            <input type="hidden" name="${Elements.TIME_PERIOD.getElemName()}" value="${currentPeriod.start.stringValue}" />
                            <input type="hidden" name="${Elements.ID_INPUT.getElemName()}" value="${te.id.value}" />
                        </form>
                        <div id="edit-buttons" class="action">
                            <button form="cancellation_form" id="${Elements.CANCEL_BUTTON.getId()}">Cancel</button>
                            <button form="delete_form" id="${Elements.DELETE_BUTTON.getId()}">Delete</button>
                            <button form="edit_time_panel" id="${Elements.SAVE_BUTTON.getId()}">Save</button>
                        </div>
    """
        }

        fun calcHoursForWeek(employee: Employee, date: Date, tru: ITimeRecordingUtilities): Time {
            return tru.getTimeForWeek(employee, date)
        }

    }


}