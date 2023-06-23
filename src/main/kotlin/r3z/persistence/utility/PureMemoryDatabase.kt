package r3z.persistence.utility

import r3z.authentication.types.Invitation
import r3z.authentication.types.Session
import r3z.authentication.types.User
import r3z.persistence.types.*
import r3z.system.config.types.SystemConfiguration
import r3z.timerecording.types.Employee
import r3z.timerecording.types.Project
import r3z.timerecording.types.SubmittedPeriod
import r3z.timerecording.types.TimeEntry


/**
 * An object-oriented strongly-typed database
 */
class PureMemoryDatabase(
    private val diskPersistence: DatabaseDiskPersistence? = null,
    private val data: Map<String, ChangeTrackingSet<*>> = mapOf()
) {

    fun stop() {
        diskPersistence?.stop()
    }

    fun copy(): PureMemoryDatabase {
        return PureMemoryDatabase(
            data = this.data.entries.map {it.key to it.value.toList().toChangeTrackingSet()}.toMap()
        )
    }

    /**
     * This method is central to accessing the database.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: IndexableSerializable> dataAccess(directoryName: String) : DataAccess<T> {
        return DataAccess(checkNotNull(data[directoryName]), diskPersistence, directoryName) as DataAccess<T>
    }

    /**
     * returns true if all the sets of data are empty
     */
    fun isEmpty(): Boolean {
        return data.entries.all { it.value.isEmpty() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PureMemoryDatabase

        if (diskPersistence != other.diskPersistence) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = diskPersistence?.hashCode() ?: 0
        result = 31 * result + data.hashCode()
        return result
    }

    companion object {

        /**
         * Creates a default empty database with our common data sets, empty
         */
        fun createEmptyDatabase(diskPersistence: DatabaseDiskPersistence? = null) : PureMemoryDatabase {
            val datamap = mapOf(
                Employee.directoryName to ChangeTrackingSet<Employee>(),
                TimeEntry.directoryName to ChangeTrackingSet<TimeEntry>(),
                Project.directoryName to ChangeTrackingSet<Project>(),
                SubmittedPeriod.directoryName to ChangeTrackingSet<SubmittedPeriod>(),
                Session.directoryName to ChangeTrackingSet<Session>(),
                User.directoryName to ChangeTrackingSet<User>(),
                Invitation.directoryName to ChangeTrackingSet<Invitation>(),
                SystemConfiguration.directoryName to ChangeTrackingSet<SystemConfiguration>()
            )
            return PureMemoryDatabase(diskPersistence, data = datamap)
        }
    }

}