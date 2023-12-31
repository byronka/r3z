package r3z.timerecording

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.NO_USER
import r3z.authentication.types.SYSTEM_USER
import r3z.authentication.types.User
import r3z.authentication.utility.FakeRolesChecker
import r3z.persistence.types.DataAccess
import r3z.persistence.utility.PureMemoryDatabase.Companion.createEmptyDatabase
import r3z.system.misc.*
import r3z.timerecording.types.*
import r3z.timerecording.utility.ITimeRecordingUtilities
import r3z.timerecording.utility.TimeRecordingUtilities
import org.junit.Assert.*
import org.junit.Test

class TimeRecordingUtilitiesRoleTests {

    private val pmd = createEmptyDatabase()
    private val timeEntryDataAccess: DataAccess<TimeEntry> = pmd.dataAccess(TimeEntry.directoryName)
    private val projectDataAccess: DataAccess<Project> = pmd.dataAccess(Project.directoryName)
    private val submittedPeriodsDataAccess: DataAccess<SubmittedPeriod> = pmd.dataAccess(SubmittedPeriod.directoryName)

    /*
                        _                    _       _          _
      _ _ ___ __ _ _  _| |__ _ _ _   _ _ ___| |___  | |_ ___ __| |_ ___
     | '_/ -_) _` | || | / _` | '_| | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
     |_| \___\__, |\_,_|_\__,_|_|   |_| \___/_\___|  \__\___/__/\__/__/
             |___/
    alt-text: regular role tests
    font: small
    */
    @Test
    fun testRegularRole() {
        val (tru, frc) = makeTRU(DEFAULT_REGULAR_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertTrue(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeByName(DEFAULT_EMPLOYEE.name)
        assertTrue(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertTrue(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        submittedPeriodsDataAccess.actOn { s -> s.add(DEFAULT_SUBMITTED_PERIOD) }
        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.approveTimesheet(DEFAULT_EMPLOYEE, DEFAULT_PERIOD_START_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        projectDataAccess.actOn { p -> p.add(DEFAULT_PROJECT) }
        tru.deleteProject(DEFAULT_PROJECT)
        assertFalse(frc.roleCanDoAction)
    }

    /*
              _       _                _       _          _
      __ _ __| |_ __ (_)_ _    _ _ ___| |___  | |_ ___ __| |_ ___
     / _` / _` | '  \| | ' \  | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
     \__,_\__,_|_|_|_|_|_||_| |_| \___/_\___|  \__\___/__/\__/__/
    alt-text: admin role tests
    font: small
     */
    @Test
    fun testAdminRole() {
        val (tru, frc) = makeTRU(DEFAULT_ADMIN_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertTrue(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeByName(DEFAULT_EMPLOYEE.name)
        assertTrue(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertTrue(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        submittedPeriodsDataAccess.actOn { s -> s.add(DEFAULT_SUBMITTED_PERIOD) }
        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.approveTimesheet(DEFAULT_EMPLOYEE, DEFAULT_PERIOD_START_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.deleteEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        projectDataAccess.actOn { p -> p.add(DEFAULT_PROJECT) }
        tru.deleteProject(DEFAULT_PROJECT)
        assertTrue(frc.roleCanDoAction)
    }

    /*
             _                       _       _          _
  ____  _ __| |_ ___ _ __    _ _ ___| |___  | |_ ___ __| |_ ___
 (_-< || (_-<  _/ -_) '  \  | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 /__/\_, /__/\__\___|_|_|_| |_| \___/_\___|  \__\___/__/\__/__/
     |__/
     */
    @Test
    fun testSystemRole() {
        val (tru, frc) = makeTRU(SYSTEM_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertTrue(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertFalse(frc.roleCanDoAction)

        timeEntryDataAccess.actOn { t -> t.add(DEFAULT_TIME_ENTRY) }
        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)

        tru.findEmployeeByName(DEFAULT_EMPLOYEE.name)
        assertFalse(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertFalse(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertFalse(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        submittedPeriodsDataAccess.actOn { s -> s.add(DEFAULT_SUBMITTED_PERIOD) }
        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.approveTimesheet(DEFAULT_EMPLOYEE, DEFAULT_PERIOD_START_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        projectDataAccess.actOn { p -> p.add(DEFAULT_PROJECT) }
        tru.deleteProject(DEFAULT_PROJECT)
        assertFalse(frc.roleCanDoAction)
    }

    /*
                                               _       _          _
  __ _ _ __ _ __ _ _ _____ _____ _ _   _ _ ___| |___  | |_ ___ __| |_ ___
 / _` | '_ \ '_ \ '_/ _ \ V / -_) '_| | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 \__,_| .__/ .__/_| \___/\_/\___|_|   |_| \___/_\___|  \__\___/__/\__/__/
      |_|  |_|
     */
    @Test
    fun testApproverRole() {
        val (tru, frc) = makeTRU(DEFAULT_APPROVER_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertTrue(frc.roleCanDoAction)

        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertTrue(frc.roleCanDoAction)

        tru.findEmployeeByName(DEFAULT_EMPLOYEE.name)
        assertTrue(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertTrue(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertTrue(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertTrue(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        submittedPeriodsDataAccess.actOn { s -> s.add(DEFAULT_SUBMITTED_PERIOD) }
        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertTrue(frc.roleCanDoAction)

        tru.approveTimesheet(DEFAULT_EMPLOYEE, DEFAULT_PERIOD_START_DATE)
        assertTrue(frc.roleCanDoAction)

        tru.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        projectDataAccess.actOn { p -> p.add(DEFAULT_PROJECT) }
        tru.deleteProject(DEFAULT_PROJECT)
        assertFalse(frc.roleCanDoAction)
    }

    /*
                              _       _          _
  _ _  ___ _ _  ___   _ _ ___| |___  | |_ ___ __| |_ ___
 | ' \/ _ \ ' \/ -_) | '_/ _ \ / -_) |  _/ -_|_-<  _(_-<
 |_||_\___/_||_\___| |_| \___/_\___|  \__\___/__/\__/__/

     */
    @Test
    fun testNoneRole() {
        val (tru, frc) = makeTRU(NO_USER)

        tru.changeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.createEmployee(DEFAULT_EMPLOYEE_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createProject(DEFAULT_PROJECT_NAME)
        assertFalse(frc.roleCanDoAction)

        tru.createTimeEntry(DEFAULT_TIME_ENTRY.toTimeEntryPreDatabase())
        assertFalse(frc.roleCanDoAction)

        timeEntryDataAccess.actOn { t -> t.add(DEFAULT_TIME_ENTRY) }
        tru.deleteTimeEntry(DEFAULT_TIME_ENTRY)
        assertFalse(frc.roleCanDoAction)

        tru.findEmployeeById(DEFAULT_EMPLOYEE.id)
        assertFalse(frc.roleCanDoAction)

        tru.findEmployeeByName(DEFAULT_EMPLOYEE.name)
        assertFalse(frc.roleCanDoAction)

        tru.findProjectById(DEFAULT_PROJECT.id)
        assertFalse(frc.roleCanDoAction)

        tru.getAllEntriesForEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        tru.getEntriesForEmployeeOnDate(DEFAULT_EMPLOYEE, DEFAULT_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.getTimeEntriesForTimePeriod(DEFAULT_EMPLOYEE, DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.listAllEmployees()
        assertTrue(frc.roleCanDoAction)

        tru.listAllProjects()
        assertFalse(frc.roleCanDoAction)

        tru.submitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        submittedPeriodsDataAccess.actOn { s -> s.add(DEFAULT_SUBMITTED_PERIOD) }
        tru.unsubmitTimePeriod(DEFAULT_TIME_PERIOD)
        assertFalse(frc.roleCanDoAction)

        tru.approveTimesheet(DEFAULT_EMPLOYEE, DEFAULT_PERIOD_START_DATE)
        assertFalse(frc.roleCanDoAction)

        tru.deleteEmployee(DEFAULT_EMPLOYEE)
        assertFalse(frc.roleCanDoAction)

        projectDataAccess.actOn { p -> p.add(DEFAULT_PROJECT) }
        tru.deleteProject(DEFAULT_PROJECT)
        assertFalse(frc.roleCanDoAction)
    }

    /*
     _ _       _                  __ __        _    _           _
    | | | ___ | | ___  ___  _ _  |  \  \ ___ _| |_ | |_  ___  _| | ___
    |   |/ ._>| || . \/ ._>| '_> |     |/ ._> | |  | . |/ . \/ . |<_-<
    |_|_|\___.|_||  _/\___.|_|   |_|_|_|\___. |_|  |_|_|\___/\___|/__/
                 |_|
     alt-text: Helper Methods
     */

    private fun makeTRU(user: User = DEFAULT_ADMIN_USER): Pair<ITimeRecordingUtilities, FakeRolesChecker>{
        val frc = FakeRolesChecker()
        val tru = TimeRecordingUtilities(pmd, CurrentUser(user), testLogger, frc)
        return Pair(tru, frc)
    }
}