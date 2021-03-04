package coverosR3z

import coverosR3z.authentication.persistence.AuthenticationPersistence
import coverosR3z.authentication.types.*
import coverosR3z.authentication.utility.AuthenticationUtilities
import coverosR3z.config.utility.SystemOptions
import coverosR3z.logging.ILogger
import coverosR3z.logging.ILogger.Companion.logImperative
import coverosR3z.logging.Logger
import coverosR3z.persistence.types.ChangeTrackingSet
import coverosR3z.persistence.types.SimpleConcurrentSet
import coverosR3z.persistence.utility.DatabaseDiskPersistence
import coverosR3z.persistence.utility.PureMemoryDatabase
import coverosR3z.server.types.BusinessCode
import coverosR3z.server.types.PreparedResponseData
import coverosR3z.server.types.ServerObjects
import coverosR3z.server.utility.SSLServer
import coverosR3z.server.utility.Server
import coverosR3z.server.utility.StaticFilesUtilities
import coverosR3z.timerecording.persistence.TimeEntryPersistence
import coverosR3z.timerecording.types.*
import coverosR3z.timerecording.utility.TimeRecordingUtilities

import java.io.File
import java.net.Socket
import java.util.concurrent.*

/**
 * This serves as a central location for the code
 * needed to start the system, called by Main.
 */
class FullSystem private constructor(
    val pmd: PureMemoryDatabase,
    val businessCode: BusinessCode,
    val logger: ILogger,
) {

    lateinit var serverFuture: Future<*>
    lateinit var sslServerFuture: Future<*>
    lateinit var esForThreadsInServer: ExecutorService
    lateinit var server: Server
    lateinit var sslServer: SSLServer

    private val runningSockets : SimpleConcurrentSet<Socket> = SimpleConcurrentSet()

    fun addRunningSocket(socket : Socket) {
        runningSockets.add(socket)
        logger.logTrace { "added a socket (count: ${runningSockets.size})" }
    }

    fun removeRunningSocket(socket : Socket) {
        runningSockets.remove(socket)
        logger.logTrace{"removed a socket (count: ${runningSockets.size})"}
    }

    /**
     * this adds a hook to the Java runtime, so that if the app is running
     * and a user stops it - by pressing ctrl+c or a unix "kill" command - the
     * server socket will be shutdown and some messages about closing the server
     * will log
     */
    fun addShutdownHook(
    ) {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                shutdown()
            })
    }

    /**
     * Systematically shuts down everything in the system,
     */
    fun shutdown(
    ) {
        logImperative("Received shutdown command")
        logImperative("Looping through all sockets with a close command")
        this.runningSockets.forEach { it.close() }

        logImperative("Shutting down the database")
        pmd.stop()

        logImperative("Shutting down the non-ssl server thread")
        server.halfOpenServerSocket.close()
        serverFuture.get()

        logImperative("Waiting for the ssl server thread")
        sslServer.sslHalfOpenServerSocket.close()
        sslServerFuture.get()

        esForThreadsInServer.shutdown()
        esForThreadsInServer.awaitTermination(10, TimeUnit.SECONDS)

        logImperative("Shutting down logging")
        logger.stop()

        logImperative("Goodbye world!")
    }



    companion object {

        /**
         * Kicks off a multitude of components, including the database
         * and the server
         *
         * The parameters are just here to enable testing.  A standard system
         * can be run without any of the default params
         *
         * @param systemOptions mandatory - sets options for the system
         * @param pmd the [PureMemoryDatabase].  You only need to set this for testing.  Otherwise, ignore it
         * @param businessCode The [BusinessCode] utilities that will be used by the server.  You only need to set this for testing
         */
        fun startSystem(
            // get the user's choices from the command line
            systemOptions: SystemOptions = SystemOptions(),

            // the logger has to be one of the first things to start in the system
            logger : ILogger = Logger(),

            // start the database
            pmd : PureMemoryDatabase = makeDatabase(dbDirectory = systemOptions.dbDirectory, logger = logger),

            // create the utilities that the API's will use by instantiating
            // them with the database as a parameter
            businessCode : BusinessCode = initializeBusinessCode(pmd, logger)
        ) : FullSystem {

            logger.configureLogging(systemOptions)

            // get the static files like CSS, JS, etc loaded into a cache
            val staticFileCache = mutableMapOf<String, PreparedResponseData>()
            StaticFilesUtilities.loadStaticFilesToCache(staticFileCache)

            // create a package of data that is needed by the servers,
            // such as the static file cache
            val serverObjects = ServerObjects(staticFileCache, logger, systemOptions.port, systemOptions.sslPort, systemOptions.allowInsecure)

            // start an executor service which will handle the threads inside the server
            val esForThreadsInServer: ExecutorService = Executors.newCachedThreadPool(Executors.defaultThreadFactory())

            // instantiate a system object, we'll need this when starting the servers
            val fullSystem = FullSystem(pmd, businessCode, logger)

            // start the regular server
            val serverExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
            val server = Server(systemOptions.port, esForThreadsInServer, businessCode, serverObjects, fullSystem)
            val serverFuture = serverExecutor.submit(server.createServerThread())

            // start the ssl server
            val sslServerExecutor = Executors.newSingleThreadExecutor(Executors.defaultThreadFactory())
            val sslServer = SSLServer(systemOptions.sslPort, esForThreadsInServer, businessCode, serverObjects, fullSystem)
            val sslServerFuture = sslServerExecutor.submit(sslServer.createSecureServerThread())

            // Add an Administrator employee and role if the database is empty
            if (pmd.isEmpty()) {
                val mrAdmin = businessCode.tru.createEmployee(EmployeeName("Administrator"))
                logImperative("Created an initial employee")
                val (_, user) = businessCode.au.register(
                    UserName("administrator"),
                    Password("password12345"),
                    mrAdmin.id
                )
                businessCode.au.addRoleToUser(user, Roles.ADMIN)
                logImperative("Create an initial user")
            }

            fullSystem.serverFuture = serverFuture
            fullSystem.sslServerFuture = sslServerFuture
            fullSystem.esForThreadsInServer = esForThreadsInServer
            fullSystem.server = server
            fullSystem.sslServer = sslServer
            return fullSystem
        }


        /**
         * Set up the classes necessary for business-related actions, like
         * recording time, and so on
         */
        private fun initializeBusinessCode(
            pmd : PureMemoryDatabase?,
            logger: ILogger
        ): BusinessCode {
            checkNotNull(pmd)
            val cu = CurrentUser(SYSTEM_USER)
            val tep = TimeEntryPersistence(pmd, cu, logger)
            val tru = TimeRecordingUtilities(tep, cu, logger)

            val ap = AuthenticationPersistence(pmd, logger)
            val au = AuthenticationUtilities(ap, logger)
            return BusinessCode(tru, au)
        }

        /**
         * Initializes the database
         * @param pmd typically you would provide null here, but you can enter a value if you want to inject a mock.  If you
         *            provide a mock, this function will ignore the dbDirectory parameter.
         * @param dbDirectory the database directory.  If you provide a string the system will use that as the directory
         *                    for the disk persistence.  If you provide null then the system will operate in memory-only,
         *                    see PureMemoryDatabase.startMemoryOnly
         */
        private fun makeDatabase(
            pmd: PureMemoryDatabase? = null,
            dbDirectory: String? = null,
            logger: ILogger
        ): PureMemoryDatabase {
            logImperative("database directory is $dbDirectory")
            return pmd ?: if (dbDirectory == null) {
                val datamap = mapOf(
                    Employee.directoryName to ChangeTrackingSet<Employee>(),
                    TimeEntry.directoryName to ChangeTrackingSet<TimeEntry>(),
                    Project.directoryName to ChangeTrackingSet<Project>(),
                    SubmittedPeriod.directoryName to ChangeTrackingSet<SubmittedPeriod>(),
                    Session.directoryName to ChangeTrackingSet<Session>(),
                    User.directoryName to ChangeTrackingSet<User>()
                )
                PureMemoryDatabase(data = datamap)
            } else {
                DatabaseDiskPersistence(dbDirectory, logger).startWithDiskPersistence()
            }
        }

        /**
         * this saves a file to the home directory, SYSTEM_RUNNING,
         * that will indicate the system is active
         */
        fun createSystemRunningMarker() {
            File("SYSTEM_RUNNING").writeText("This file serves as a marker to indicate the system is running.")
            File("SYSTEM_RUNNING").deleteOnExit()
        }

    }
}