package r3z.system.config.persistence

import r3z.system.config.types.SystemConfiguration

interface ISystemConfigurationPersistence {
    fun setSystemConfig(sysConfig: SystemConfiguration)
    fun getSystemConfig(): SystemConfiguration?

}
