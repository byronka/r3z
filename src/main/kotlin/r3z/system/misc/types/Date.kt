package r3z.system.misc.types

import r3z.system.misc.utility.checkParseToInt
import java.time.LocalDate
import java.text.SimpleDateFormat




const val dateNotNullMsg = "date must not be null"
const val dateNotBlankMsg = "date must not be blank"
val earliestAllowableDate: LocalDate = LocalDate.of(1980, 1, 1)
val latestAllowableDate: LocalDate = LocalDate.of(2200, 1, 1)

/**
 * This is used to represent nothing - just to avoid using null
 * It's a typed null, essentially
 */
val NO_DATE = Date(earliestAllowableDate.toEpochDay())

enum class DayOfWeek {
    Sunday,
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday
}

/**
 * Given an epochDay, returns the [DayOfWeek]
 *
 *  we'll find the day of the week by understanding that the days of the week have
 *  continued in an orderly fashion since starting at epoch day zero.  That first
 *  day was a Thursday, as it happens.  Since we've had no duplicate days of the week
 *  since then (we've never had, for example, a Tuesday immediately followed by a Tuesday)
 *  we can figure out today's day of the week by a bit of modulo math.
 *
 *  the day of the week, by example:
 *  assume the epoch day today is zero.
 *  assume the days Sunday through Saturday are labeled 0 through 6:
 *  Sunday is 0
 *  Monday is 1
 *  Tuesday is 2
 *  Wednesday is 3
 *  Thursday is 4
 *  Friday is 5
 *  Saturday is 6
 *
 *  0 + 4 = 4 -> Thursday, as expected
 *  7 + 4 = 11 -> 11 modulo 7 is 4 -> Thursday
 *
 *
 */
fun dayOfWeekCalc(epochDay: Long): DayOfWeek {
    return DayOfWeek.values()[(epochDay + 4).rem(7).toInt()]
}

fun calculateSundayDate(today: Date): Date {
    val dayOfWeek = dayOfWeekCalc(today.epochDay)
    return Date(today.epochDay - dayOfWeek.ordinal)
}

enum class Month(val ord: Int) {
    JAN(1), FEB(2), MAR(3), APR(4), MAY(5), JUN(6),
    JUL(7), AUG(8), SEP(9), OCT(10), NOV(11), DEC(12);

    /**
     * Give the length of the month the provided date is within
     */
    fun calculateLength(year: Int): Int {
        return LocalDate.of(year, this.ord, 1).lengthOfMonth()
    }

    companion object {
        fun from(ord: Int): Month {
            check (ord in 1..12) { "Month must comply with arbitrary human divisions of time into 12 similarly sized chunks a year. You tried $ord."}
            return values().single { it.ord == ord }
        }
    }
}

/**
 * A simplistic representation of date.
 *
 * internal data is merely the number of days since the epoch - 1970-01-01
 */

class Date(val epochDay : Long) : Comparable<Date> {
    constructor(year: Int, month: Month, day: Int) : this(LocalDate.of(year, month.ord, day).toEpochDay())

    /**
     * Normal format "YYYY-MM-DD"
     */
    val stringValue = java.sql.Date.valueOf(LocalDate.ofEpochDay(epochDay)).toString()

    val viewTimeHeaderFormat: String = SimpleDateFormat("EEE, MMM d").format(java.sql.Date.valueOf(LocalDate.ofEpochDay(
        epochDay
    )))

    init {
        val beginDate = earliestAllowableDate
        val endDate = latestAllowableDate
        require(epochDay in (beginDate.toEpochDay())..(endDate.toEpochDay())) {
            "no way on earth people are using this before $beginDate or past $endDate, you had a date of $stringValue"
        }
    }

    fun day() : Int {
        return LocalDate.ofEpochDay(epochDay).dayOfMonth
    }

    fun month() : Int {
        return LocalDate.ofEpochDay(epochDay).monthValue
    }

    fun year() : Int {
        return LocalDate.ofEpochDay(epochDay).year
    }


    /**
     * All we care about is the epoch day, making
     * this as simple as it can get.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Date

        if (epochDay != other.epochDay) return false

        return true
    }

    override fun hashCode(): Int {
        return epochDay.hashCode()
    }

    override fun toString(): String {
        return "Date(epochDay=$epochDay, $stringValue)"
    }

    override fun compareTo(other: Date): Int {
        return this.epochDay.compareTo(other.epochDay)
    }

    companion object {

        // get the date right now
        fun now(): Date {
            return Date(LocalDate.now().toEpochDay())
        }

        /**
         * Takes a string value in a format like: "2020-06-20"
         * and returns a date.
         */
        fun make(value: String?): Date {
            val valueNotNull = checkNotNull(value){ dateNotNullMsg }
            require(valueNotNull.isNotBlank()) { dateNotBlankMsg }
            val split = value.split("-")
            check(split.count() == 3) {"Input to this function must split to exactly three parts"}

            return Date(checkParseToInt(split[0]), Month.from(checkParseToInt(split[1])), checkParseToInt(split[2]))
        }


    }

}