package r3z.server.types

import r3z.system.misc.utility.toBytes
import r3z.system.misc.utility.toStr

/**
 * Data for shipping to the client
 */
data class PreparedResponseData(val fileContents: ByteArray,
                                val statusCode: StatusCode,

                                /**
                                 * these are the headers that a particular API set.
                                 * There are also server-wide headers set at ServerUtilities.returnData
                                 */
                                val headers : List<String> = emptyList()){

    constructor(fileContents: String, statusCode: StatusCode, headers : List<String> = emptyList())
            : this(toBytes(fileContents), statusCode, headers)

    fun fileContentsString() : String {
        return toStr(fileContents)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreparedResponseData

        if (!fileContents.contentEquals(other.fileContents)) return false
        if (statusCode != other.statusCode) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileContents.contentHashCode()
        result = 31 * result + statusCode.hashCode()
        result = 31 * result + headers.hashCode()
        return result
    }


}