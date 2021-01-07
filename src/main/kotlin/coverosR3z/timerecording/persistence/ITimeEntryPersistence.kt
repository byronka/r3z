package coverosR3z.timerecording.persistence

import coverosR3z.misc.types.Date
import coverosR3z.timerecording.types.*

interface ITimeEntryPersistence {
    fun persistNewTimeEntry(entry: TimeEntryPreDatabase) : TimeEntry
    fun persistNewProject(projectName: ProjectName) : Project
    fun persistNewEmployee(employeename: EmployeeName): Employee

    /**
     * Provided a employee and date, give the number of minutes they worked on that date
     */
    fun queryMinutesRecorded(employee: Employee, date: Date): Time

    /**
     * Get all the time entries for a particular employee
     */
    fun readTimeEntries(employee: Employee): Set<TimeEntry>
    fun readTimeEntriesOnDate(employee: Employee, date: Date): Set<TimeEntry>
    fun getProjectByName(name : ProjectName) : Project
    fun getProjectById(id: ProjectId): Project
    fun getAllProjects(): List<Project>
    fun getAllEmployees(): List<Employee>
    fun getEmployeeById(id: EmployeeId): Employee
    fun overwriteTimeEntry(empId: EmployeeId, id: Int, newEntry: TimeEntry)
}