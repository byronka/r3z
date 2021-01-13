package coverosR3z.uitests

import coverosR3z.BDDHelpers
import coverosR3z.DEFAULT_DATE_STRING
import coverosR3z.DEFAULT_PASSWORD
import coverosR3z.UITest
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.utility.Server
import coverosR3z.timerecording.api.ViewTimeAPI
import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.*
import org.junit.Assert.assertEquals
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

class UIRecordTime {

    /*
    Employee user story:
         As an employee, Andrea
         I want to record my time
         So that I am easily able to document my time in an organized way
    */

    @UITest
    @Test
    fun `recordTime - An employee should be able to enter time for a specified date`() {
        loginAsUserAndCreateProject("alice", "projecta")
        recordTime.markDone("Given the employee worked 8 hours yesterday,")

        enterTimeForEmployee("projecta")
        recordTime.markDone("when the employee enters their time,")

        verifyTheEntry()
        recordTime.markDone("then time is saved.")

        logout()
    }

    //TODO: Implement this test for real
    @UITest
    @Test
    fun `recordTime - An employee should be able to edit the number of hours worked from a previous time entry` () {
        loginAsUserAndCreateProject("Andrea", "projectb")
        recordTime.markDone("Given Andrea has a previous time entry with 1 hour,")

        // when the employee enters their time
        enterTimeForEmployee("projectb")

        driver.get("$domain/${ViewTimeAPI.path}")
        recordTime.markDone("when she changes the entry to two hours,")
        // muck with it

        val timeField = driver.findElement(By.cssSelector("#time-entry-1-1 .time input"))
        timeField.sendKeys("120")
        // change time to 120

        driver.get("$domain/${ViewTimeAPI.path}")
        val expected = 60120 // debugging, should eventually HIGHLIGHT and replace
        assertEquals(expected, driver.findElement(By.cssSelector("#time-entry-1-1 .time input")).getAttribute("value"))
        // stopping point 12/10/20: sent keys do not persist when the driver accesses the page again. Won't solve that
        // until we persist it in some way
        logout()
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
        private const val port = 2001
        private const val domain = "http://localhost:$port"
        private val webDriver = Drivers.CHROME
        private lateinit var sc : Server
        private lateinit var driver: WebDriver
        private lateinit var rp : RegisterPage
        private lateinit var lp : LoginPage
        private lateinit var llp : LoggingPage
        private lateinit var etp : EnterTimePage
        private lateinit var eep : EnterEmployeePage
        private lateinit var epp : EnterProjectPage
        private lateinit var lop : LogoutPage
        private lateinit var createEmployee : BDDHelpers
        private lateinit var recordTime : BDDHelpers
        private lateinit var businessCode : BusinessCode
        private lateinit var pmd : PureMemoryDatabase

        @BeforeClass
        @JvmStatic
        fun setup() {
            // install the most-recent chromedriver
            WebDriverManager.chromedriver().setup()
            WebDriverManager.firefoxdriver().setup()

            // setup for BDD
            createEmployee = BDDHelpers("createEmployeeBDD.html")
            recordTime = BDDHelpers("enteringTimeBDD.html")
        }

        @AfterClass
        @JvmStatic
        fun shutDown() {
            createEmployee.writeToFile()
            recordTime.writeToFile()

        }

    }

    @Before
    fun init() {
        // start the server
        sc = Server(port)
        pmd = Server.makeDatabase()
        businessCode = Server.initializeBusinessCode(pmd)
        sc.startServer(businessCode)

        driver = webDriver.driver()

        rp = RegisterPage(driver, domain)
        lp = LoginPage(driver, domain)
        etp = EnterTimePage(driver, domain)
        eep = EnterEmployeePage(driver, domain)
        epp = EnterProjectPage(driver, domain)
        llp = LoggingPage(driver, domain)
        lop = LogoutPage(driver, domain)

    }

    @After
    fun cleanup() {
        sc.halfOpenServerSocket.close()
        driver.quit()
    }

    private fun logout() {
        lop.go()
    }

    private fun enterTimeForEmployee(project: String) {
        val dateString = if (driver is ChromeDriver) {
            "06122020"
        } else {
            DEFAULT_DATE_STRING
        }

        // Enter time
        etp.enterTime(project, "60", "", dateString)
    }

    private fun loginAsUserAndCreateProject(user: String, project: String) {
        val password = DEFAULT_PASSWORD.value

        // register and login
        rp.register(user, password, "Administrator")
        lp.login(user, password)

        // Create project
        epp.enter(project)
    }

    private fun verifyTheEntry() {
        // Verify the entry
        driver.get("$domain/${ViewTimeAPI.path}")
        assertEquals("your time entries", driver.title)
        assertEquals("2020-06-12", driver.findElement(By.cssSelector("body > table > tbody > tr > td:nth-child(4)")).text)
    }


}