package r3z.server.types

import r3z.system.logging.ILogger
import r3z.system.config.persistence.ISystemConfigurationPersistence

/**
 * Data needed by a server that isn't business-related
 */
data class ServerObjects(
    val staticFileCache: Map<String, PreparedResponseData>,
    val logger: ILogger,
    /**
     * Regular non-secure port for the server
     */
    val port: Int,
    val sslPort: Int,

    /**
     * If this is true, we allow clients to remain on the
     * non-secure http endpoint without redirecting them
     * to the https secure port
     */
    val allowInsecureUsage: Boolean,

    /**
     * This is extracted from the Host header we receive from the browser.
     * Call us whatever you want, as long as you don't call us late for supper.
     */
    val host: String = "",

    /**
     * This is used to store changes to the system's
     * configuration that may take place during run-time,
     * for example, the logging settings.
     */
    val scp: ISystemConfigurationPersistence,

    /**
     * The length of time, in milliseconds, for the socket to wait
     * before crash closing
     * 10 seconds worth is pretty ordinary
     */
    val socketTimeout: Int
    )