package com.coveros.r3z.domainobjects

const val MAX_DETAILS_LENGTH = 500

data class Details(val value : String = "") {
    init {
        // This max length is reinforced in V1_Create_person_table.sql
        assert(value.length <= MAX_DETAILS_LENGTH) { "no reason why details for a time entry would ever need to be too big. " +
                "if you have more to say than the lord's prayer, you're probably doing it wrong." }
    }
}