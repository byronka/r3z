package r3z.timerecording.exceptions

/**
 * If the system encounters the situation that a employee
 * is trying to add more time in such a way that the new total
 * will end up being more than 24 hours for a single day,
 * that's insane.
 */
class ExceededDailyHoursAmountException : Exception("Exceeded number of hours in a day on this time entry")