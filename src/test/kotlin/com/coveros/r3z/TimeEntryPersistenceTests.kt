package com.coveros.r3z

import com.coveros.r3z.domainobjects.*
import com.coveros.r3z.persistence.getMemoryBasedDatabaseConnectionPool
import com.coveros.r3z.persistence.microorm.DbAccessHelper
import com.coveros.r3z.persistence.microorm.IDbAccessHelper
import com.coveros.r3z.timerecording.TimeEntryPersistence
import org.h2.jdbc.JdbcSQLDataException
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

const val MAX_DETAIL_TEXT_LENGTH = 500

class TimeEntryPersistenceTests {

    @Test fun `can record a time entry to the database`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val expectedNewId : Long = 1
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val result = tep.persistNewTimeEntry(createTimeEntry(project = newProject))

        val message = "we expect that the insertion of a new row will return the new id"

        assertEquals(message, expectedNewId, result)
    }

    @Test fun `can get all time entries by a user`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val userName = "test"
        val user = User(1L, userName)
        tep.persistNewUser(userName)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val entry1 = createTimeEntry(user = user, project = newProject)
        val entry2 = createTimeEntry(user = user, project = newProject)
        tep.persistNewTimeEntry(entry1)
        tep.persistNewTimeEntry(entry2)
        val expectedResult = listOf(entry1, entry2)

        val actualResult = tep.readTimeEntries(user) ?: listOf()

        val msg = "what we entered and what we get back should be identical, instead got"
        assertEquals(msg, expectedResult, actualResult)
    }

    /**
     * If we try to add a time entry with a project id that doesn't exist in
     * the database, we should get an exception back from the database
     */
    @Test fun `Can't record a time entry that has a nonexistent project id`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        Assert.assertThrows(JdbcSQLIntegrityConstraintViolationException::class.java) {
            tep.persistNewTimeEntry(createTimeEntry())
        }
    }

    /**
     * Details only takes up to MAX_DETAIL_TEXT_LENGTH characters.  Any more and an exception will be thrown.
     */
    @Test fun `Can't record a time entry with too many letters in details`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))

        Assert.assertThrows(JdbcSQLDataException::class.java) {
            dbAccessHelper.executeInsert(
                    "Creates a new time entry in the database - a record of a particular users's time on a project",
                    "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?, ?, ?, ?);",
                    1, newProject.id, 60, A_RANDOM_DAY_IN_JUNE_2020.sqlDate, "a".repeat(MAX_DETAIL_TEXT_LENGTH + 1))
        }
    }

    /**
     * Details only takes up to MAX_DETAIL_TEXT_LENGTH characters.  Any more and an exception will be thrown.
     * This checks the unicode version of that idea.
     */
    @Test fun `Can't record a time entry with too many letters in details, using unicode`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        // the following unicode is longer than it seems.  Thanks Matt!
        val unicodeWeirdCharacters = "h̬͕̘ͅiͅ ̘͔̝̺̩͚͚̕b̧͈̙͕̰̖̯y̡̺r̙o̦̯̙͎̮n̯̘̣͖"
        // we want to think about the MAX_DETAIL_TEXT_LENGTH character limit for the details varchar field,
        // how does it relate to unicode chars?  Here, we'll calculate the number of times
        // we think it should fit into the given maximum character length, plus one in
        // order to bust through the ceiling.

        val numberTimesToRepeat = MAX_DETAIL_TEXT_LENGTH/unicodeWeirdCharacters.length + 1
        // as of the time of writing, numberTimesToRepeat was 13
        print("only have to repeat $numberTimesToRepeat times for this to bust the ceiling")

        Assert.assertThrows(JdbcSQLDataException::class.java) {
            dbAccessHelper.executeInsert(
                    "Creates a new time entry in the database - a record of a particular users's time on a project",
                    "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?, ?, ?, ?);",
                    1, newProject.id, 60, A_RANDOM_DAY_IN_JUNE_2020.sqlDate, unicodeWeirdCharacters.repeat(numberTimesToRepeat))
        }
    }

    /**
     * Make sure that when we use really weird unicode, it's accepted
     */
    @Test fun `Can record a time entry with unicode letters in details`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))

        val newId = dbAccessHelper.executeInsert(
                "Creates a new time entry in the database - a record of a particular users's time on a project",
                "INSERT INTO TIMEANDEXPENSES.TIMEENTRY (user, project, time_in_minutes, date, details) VALUES (?, ?, ?, ?, ?);",
                1, newProject.id, 60, A_RANDOM_DAY_IN_JUNE_2020.sqlDate, "h̬͕̘ͅiͅ ̘͔̝̺̩͚͚̕b̧͈̙͕̰̖̯y̡̺r̙o̦̯̙͎̮n̯̘̣͖")
        assertEquals("we should get a new id for the new timeentry", 1, newId)
    }

    /**
     * Can we store data from non-English alphabets? you betcha
     */
    @Test fun `Can record a time entry with unicode chars`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val expectedNewId : Long = 1
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val result = tep.persistNewTimeEntry(createTimeEntry(
            project = newProject,
            details = Details(" Γεια σου κόσμε! こんにちは世界 世界，你好")
        ))

        val message = "we expect that the insertion of a new row will return the new id"
        assertEquals(message, expectedNewId, result)
    }

    /**
     * We need to be able to know how many hours a user has worked for the purpose of validation
     */
    @Test
    fun `Can query hours worked by a user on a given day`() {
        val dbAccessHelper = initializeDatabaseForTest()
        val tep = TimeEntryPersistence(dbAccessHelper)
        val newProject = tep.persistNewProject(ProjectName("test project"))
        val testUser = User(1, "test")
        tep.persistNewTimeEntry(createTimeEntry(user=testUser, time= Time(60), project=newProject, date = A_RANDOM_DAY_IN_JUNE_2020))

        val query = tep.queryMinutesRecorded(user=testUser, date=A_RANDOM_DAY_IN_JUNE_2020)
        assertEquals(60L, query)
    }

    private fun initializeDatabaseForTest() : IDbAccessHelper {
        val ds = getMemoryBasedDatabaseConnectionPool()
        val dbAccessHelper: IDbAccessHelper =
                DbAccessHelper(ds)
        dbAccessHelper.cleanDatabase()
        dbAccessHelper.migrateDatabase()
        return dbAccessHelper
    }
}