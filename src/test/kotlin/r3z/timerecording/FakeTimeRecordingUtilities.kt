package r3z.timerecording

import r3z.authentication.types.CurrentUser
import r3z.system.misc.*
import r3z.system.misc.types.Date
import r3z.timerecording.types.*
import r3z.timerecording.utility.ITimeRecordingUtilities

/**
 * Used as a mock object for testing
 */
class FakeTimeRecordingUtilities(
    var createTimeEntryBehavior : () -> RecordTimeResult = { RecordTimeResult() },
    var changeEntryBehavior : () -> RecordTimeResult = { RecordTimeResult() },
    var createProjectBehavior : () -> Project = { DEFAULT_PROJECT },
    var deleteProjectBehavior : () -> DeleteProjectResult = { DeleteProjectResult.SUCCESS },
    var createEmployeeBehavior : () -> Employee = { DEFAULT_EMPLOYEE },
    var deleteEmployeeBehavior : () -> Boolean = { true },
    var getEntriesForEmployeeOnDateBehavior : () -> Set<TimeEntry> = { emptySet() },
    var getAllEntriesForEmployeeBehavior : () -> Set<TimeEntry> = { emptySet() },
    var getTimeEntriesForTimePeriodBehavior : () -> Set<TimeEntry> = { emptySet() },
    var changeUserBehavior : () -> ITimeRecordingUtilities = { FakeTimeRecordingUtilities() },
    var listAllProjectsBehavior : () -> List<Project> = { emptyList() },
    var findProjectByIdBehavior : () -> Project = { NO_PROJECT },
    var findProjectByNameBehavior : () -> Project = { NO_PROJECT },
    var listAllEmployeesBehavior : () -> List<Employee> = { emptyList() },
    var findEmployeeByIdBehavior : () -> Employee = { NO_EMPLOYEE },
    var findEmployeeByNameBehavior : () -> Employee = { NO_EMPLOYEE },
    var submitTimePeriodBehavior : () -> SubmittedPeriod = { DEFAULT_SUBMITTED_PERIOD },
    var unsubmitTimePeriodBehavior : () -> Unit = {},
    var getSubmittedTimePeriodBehavior : () -> SubmittedPeriod = { DEFAULT_SUBMITTED_PERIOD },
    var isInASubmittedPeriodBehavior : () -> Boolean = { false },
    var deleteTimeEntryBehavior : () -> Boolean = { true },
    var findTimeEntryByIdBehavior : () -> TimeEntry = { DEFAULT_TIME_ENTRY },
    var approveTimesheetBehavior: () -> ApprovalResultStatus = { ApprovalResultStatus.SUCCESS },
    var unapproveTimesheetBehavior: () -> ApprovalResultStatus = { ApprovalResultStatus.SUCCESS },
    var isApprovedBehavior: () -> ApprovalStatus = { ApprovalStatus.UNAPPROVED },
    var isProjectUsedForTimeEntryBehavior: () -> Boolean = { true },
    var getTimeForWeekBehavior: () -> Time = { DEFAULT_TIME },
    ) : ITimeRecordingUtilities {

    override fun changeUser(cu: CurrentUser): ITimeRecordingUtilities {
        return changeUserBehavior()
    }

    override fun createTimeEntry(entry: TimeEntryPreDatabase): RecordTimeResult {
        return createTimeEntryBehavior()
    }

    override fun changeEntry(entry: TimeEntry): RecordTimeResult{
        return changeEntryBehavior()
    }

    override fun createProject(projectName: ProjectName): Project {
        return createProjectBehavior()
    }

    override fun createEmployee(employeename: EmployeeName): Employee {
        return createEmployeeBehavior()
    }

    override fun getEntriesForEmployeeOnDate(employee: Employee, date: Date): Set<TimeEntry> {
        return getEntriesForEmployeeOnDateBehavior()
    }

    override fun getAllEntriesForEmployee(employee: Employee): Set<TimeEntry> {
        return getAllEntriesForEmployeeBehavior()
    }

    override fun listAllProjects(): List<Project> {
        return listAllProjectsBehavior()
    }

    override fun findProjectById(id: ProjectId): Project {
        return findProjectByIdBehavior()
    }

    override fun findEmployeeById(id: EmployeeId): Employee {
        return findEmployeeByIdBehavior()
    }

    override fun findEmployeeByName(name: EmployeeName): Employee {
        return findEmployeeByNameBehavior()
    }

    override fun listAllEmployees(): List<Employee> {
        return listAllEmployeesBehavior()
    }
    override fun submitTimePeriod(timePeriod: TimePeriod): SubmittedPeriod{
        return submitTimePeriodBehavior()
    }

    override fun unsubmitTimePeriod(timePeriod: TimePeriod) {
        unsubmitTimePeriodBehavior()
    }

    override fun getSubmittedTimePeriod(timePeriod: TimePeriod): SubmittedPeriod {
        return getSubmittedTimePeriodBehavior()
    }

    override fun getTimeEntriesForTimePeriod(employee: Employee, timePeriod: TimePeriod): Set<TimeEntry> {
        return getTimeEntriesForTimePeriodBehavior()
    }

    override fun isInASubmittedPeriod(employee: Employee, date: Date): Boolean {
        return isInASubmittedPeriodBehavior()
    }

    override fun deleteTimeEntry(timeEntry: TimeEntry): Boolean {
        return deleteTimeEntryBehavior()
    }

    override fun findTimeEntryById(id: TimeEntryId): TimeEntry {
        return findTimeEntryByIdBehavior()
    }

    override fun approveTimesheet(employee: Employee, startDate: Date) : ApprovalResultStatus {
        return approveTimesheetBehavior()
    }

    override fun isApproved(employee: Employee, startDate: Date): ApprovalStatus {
        return isApprovedBehavior()
    }

    override fun unapproveTimesheet(employee: Employee, startDate: Date): ApprovalResultStatus {
        return unapproveTimesheetBehavior()
    }

    override fun findProjectByName(name: ProjectName): Project {
        return findProjectByNameBehavior()
    }

    override fun deleteProject(project: Project): DeleteProjectResult {
        return deleteProjectBehavior()
    }

    override fun deleteEmployee(employee: Employee): Boolean {
        return deleteEmployeeBehavior()
    }

    override fun isProjectUsedForTimeEntry(project: Project): Boolean {
        return isProjectUsedForTimeEntryBehavior()
    }

    override fun getTimeForWeek(employee: Employee, todayDate: Date): Time {
        return getTimeForWeekBehavior()
    }
}