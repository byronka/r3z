package r3z.system.logging

import r3z.authentication.types.CurrentUser
import r3z.authentication.types.SYSTEM_USER
import r3z.system.config.utility.SystemOptions
import r3z.system.config.types.SystemConfiguration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface ILogger {
    var logSettings: SystemConfiguration.LogSettings

    /**
     * Set the system to standard configuration for which
     * log entries will print
     */
    fun turnOnAllLogging()
    fun turnOffAllLogging()
    fun logAudit(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Used to log finicky details of technical solutions
     */
    fun logDebug(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Logs nearly extraneous levels of detail.
     */
    fun logTrace(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Logs items that could be concerning to the operations team.  Like
     * a missing database file.
     */
    fun logWarn(cu: CurrentUser = CurrentUser(SYSTEM_USER), msg: () -> String)

    /**
     * Sets logging per the [SystemOptions], if the user requested
     * something
     */
    fun configureLogging(serverOptions: SystemOptions)
    fun stop()

    companion object {
        /**
         * Logging that must be shown, which you cannot turn off
         */
        fun logImperative(msg: String) {
            println("${getTimestamp()} IMPERATIVE: $msg")
        }

        fun getTimestamp() : String {
            return ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
        }
    }
}