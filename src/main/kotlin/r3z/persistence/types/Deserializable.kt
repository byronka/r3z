package r3z.persistence.types

/**
 * Used for making deserialization generic across
 * types.  See [deserialize]
 */
interface Deserializable<T> {

    /**
     * Takes a string form of a type and
     * converts it to its type.
     * See [r3z.persistence.utility.DatabaseDiskPersistence.dbentryDeserialize]
     */
    fun deserialize(str: String) : T

}