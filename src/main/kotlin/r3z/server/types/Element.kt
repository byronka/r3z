package r3z.server.types

/**
 * Defines the interface to an HTML element
 */
interface Element {
    fun getElemName() : String
    fun getId() : String
    fun getElemClass(): String
}