package r3z.authentication.types

import r3z.system.misc.types.DateTime
import r3z.system.misc.utility.checkParseToInt
import r3z.system.misc.utility.checkParseToLong
import r3z.persistence.exceptions.DatabaseCorruptedException
import r3z.persistence.types.Deserializable
import r3z.persistence.types.IndexableSerializable
import r3z.persistence.types.SerializableCompanion
import r3z.persistence.types.SerializationKeys
import r3z.persistence.utility.DatabaseDiskPersistence.Companion.dbentryDeserialize

/**
 * This stores the information about when a user successfully logged
 * into the system.
 * @param sessionId the text identifier given to the user as a cookie, like "abc123",
 *        usually in a form like this: cookie: sessionId=abc123
 * @param user the user who is logged in
 * @param dt the date and time the user successfully logged in
 */
data class Session(val simpleId: Int, val sessionId: String, val user: User, val dt: DateTime) : IndexableSerializable() {

    override fun getIndex(): Int {
        return simpleId
    }

    override val dataMappings: Map<SerializationKeys, String>
        get() = mapOf(
            Keys.SIMPLE_ID to "$simpleId",
            Keys.SESSION_ID to sessionId,
            Keys.USER_ID to "${user.id.value}",
            Keys.EPOCH_SECOND to "${dt.epochSecond}"
        )

    class Deserializer(val users: Set<User>) : Deserializable<Session> {

        override fun deserialize(str: String): Session {
            return dbentryDeserialize(str, Companion) { entries ->
                val simpleId = checkParseToInt(entries[Keys.SIMPLE_ID])
                val sessionString = checkNotNull(entries[Keys.SESSION_ID])
                val id = checkParseToInt(entries[Keys.USER_ID])
                val epochSecond = checkParseToLong(entries[Keys.EPOCH_SECOND])
                val user = try {
                    users.single { it.id.value == id }
                } catch (ex: NoSuchElementException) {
                    throw DatabaseCorruptedException("Unable to find a user with the id of $id.  User set size: ${users.size}")
                }
                Session(simpleId, sessionString, user, DateTime(epochSecond))
            }
        }
    }

    companion object : SerializableCompanion<Keys>(Keys.values()) {

        override val directoryName: String
            get() = "sessions"

    }

    enum class Keys(override val keyString: String) : SerializationKeys {
        SIMPLE_ID("sid"),
        SESSION_ID("s"),
        USER_ID("id"),
        EPOCH_SECOND("e");
    }


}
