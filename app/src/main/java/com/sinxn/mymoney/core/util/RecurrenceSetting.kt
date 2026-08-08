package com.sinxn.mymoney.core.util

import android.content.Context
import com.sinxn.mymoney.R
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.Weekday
import org.dmfs.rfc5545.recur.Freq
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecurrenceSetting(
    val startDate: Date,
    val recurrenceRule: RecurrenceRule
) {

    companion object {
        const val TYPE_DAILY = 1
        const val TYPE_WEEKLY = 2
        const val TYPE_MONTHLY = 3
        const val TYPE_YEARLY = 4

        const val END_FOREVER = 1
        const val END_UNTIL = 2
        const val END_FOR = 3

        @Throws(InvalidRecurrenceRuleException::class)
        fun build(startDate: Date, rule: String): RecurrenceSetting {
            val recurrenceRule = RecurrenceRule(rule)
            return RecurrenceSetting(startDate, recurrenceRule)
        }

        fun fromStringOrFallback(startDate: Date, ruleStr: String): RecurrenceSetting {
            return try {
                RecurrenceSetting(startDate, RecurrenceRule(ruleStr))
            } catch (e: Exception) {
                RecurrenceSetting(startDate, RecurrenceRule(Freq.DAILY))
            }
        }

        fun getFixedDateTime(date: Date): DateTime {
            val calendar = Calendar.getInstance().apply { time = date }
            return DateTime(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun getFixedDate(dateTime: DateTime): Date {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, dateTime.year)
                set(Calendar.MONTH, dateTime.month)
                set(Calendar.DAY_OF_MONTH, dateTime.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.time
        }
    }

    constructor(startDate: Date, type: Int) : this(startDate, RecurrenceRule(getFreq(type)))

    val type: Int
        get() = when (recurrenceRule.freq) {
            Freq.DAILY -> TYPE_DAILY
            Freq.WEEKLY -> TYPE_WEEKLY
            Freq.MONTHLY -> TYPE_MONTHLY
            Freq.YEARLY -> TYPE_YEARLY
            else -> TYPE_DAILY
        }

    val offsetValue: Int
        get() = recurrenceRule.interval

    val weekDays: BooleanArray
        get() {
            val days = BooleanArray(7) { false }
            val part = recurrenceRule.byDayPart
            if (!part.isNullOrEmpty()) {
                for (wd in part) {
                    when (wd.weekday) {
                        Weekday.SU -> days[0] = true
                        Weekday.MO -> days[1] = true
                        Weekday.TU -> days[2] = true
                        Weekday.WE -> days[3] = true
                        Weekday.TH -> days[4] = true
                        Weekday.FR -> days[5] = true
                        Weekday.SA -> days[6] = true
                    }
                }
            } else {
                val cal = Calendar.getInstance().apply { time = startDate }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                days[dayOfWeek - 1] = true
            }
            return days
        }

    val endType: Int
        get() = when {
            recurrenceRule.isInfinite -> END_FOREVER
            recurrenceRule.until != null -> END_UNTIL
            else -> END_FOR
        }

    val endDate: Date
        get() {
            val until = recurrenceRule.until
            return if (until != null) getFixedDate(until) else startDate
        }

    val occurrenceValue: Int
        get() = recurrenceRule.count ?: 1

    val rule: String
        get() = recurrenceRule.toString()

    fun getUserReadableString(context: Context): String {
        val builder = StringBuilder()
        val offset = recurrenceRule.interval
        when (recurrenceRule.freq) {
            Freq.DAILY -> {
                if (offset <= 1) {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_day))
                } else {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_n_days, offset))
                }
            }
            Freq.WEEKLY -> {
                if (offset <= 1) {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_week))
                } else {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_n_weeks, offset))
                }
                val days = weekDays
                if (days[0]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_sunday))
                if (days[1]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_monday))
                if (days[2]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_tuesday))
                if (days[3]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_wednesday))
                if (days[4]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_thursday))
                if (days[5]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_friday))
                if (days[6]) builder.append(" " + context.getString(R.string.recurrence_hint_repeat_weekly_saturday))
            }
            Freq.MONTHLY -> {
                if (offset <= 1) {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_month))
                } else {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_n_months, offset))
                }
            }
            Freq.YEARLY -> {
                if (offset <= 1) {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_year))
                } else {
                    builder.append(context.getString(R.string.recurrence_hint_repeat_every_n_years, offset))
                }
            }
            else -> {}
        }

        val dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
        val formattedStart = dateFormat.format(startDate)
        builder.append(" ").append(context.getString(R.string.recurrence_hint_repeat_starting_from, formattedStart))

        if (recurrenceRule.isInfinite) {
            builder.append(" ").append(context.getString(R.string.recurrence_hint_repeat_forever))
        } else if (recurrenceRule.until != null) {
            val formattedEnd = dateFormat.format(endDate)
            builder.append(" ").append(context.getString(R.string.recurrence_hint_repeat_until, formattedEnd))
        } else {
            val count = recurrenceRule.count ?: 1
            if (count <= 1) {
                builder.append(" ").append(context.getString(R.string.recurrence_hint_repeat_for_one_occurrence))
            } else {
                builder.append(" ").append(context.getString(R.string.recurrence_hint_repeat_for_n_occurrence, count))
            }
        }
        return builder.toString()
    }

    fun getNextOccurrence(lastOccurrence: Date): Date? {
        val startDateTime = getFixedDateTime(startDate)
        val lastDateTime = getFixedDateTime(lastOccurrence)
        var nextDateTime: DateTime? = null
        val iterator: RecurrenceRuleIterator = recurrenceRule.iterator(startDateTime)
        while (iterator.hasNext()) {
            val nextInstance = iterator.nextDateTime()
            if (nextInstance.after(lastDateTime)) {
                nextDateTime = nextInstance
                break
            }
        }
        return nextDateTime?.let { getFixedDate(it) }
    }

    class Builder(val startDate: Date, type: Int) {
        private val recurrenceRule = RecurrenceRule(getFreq(type))

        fun setOffset(offset: Int): Builder {
            recurrenceRule.interval = offset
            return this
        }

        fun setRepeatWeekDay(weekDays: BooleanArray): Builder {
            val list = mutableListOf<RecurrenceRule.WeekdayNum>()
            if (weekDays[0]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.SU))
            if (weekDays[1]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.MO))
            if (weekDays[2]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.TU))
            if (weekDays[3]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.WE))
            if (weekDays[4]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.TH))
            if (weekDays[5]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.FR))
            if (weekDays[6]) list.add(RecurrenceRule.WeekdayNum(0, Weekday.SA))
            recurrenceRule.byDayPart = list
            return this
        }

        fun setRepeatSameMonthDay(): Builder {
            val cal = Calendar.getInstance().apply { time = startDate }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            recurrenceRule.setByPart(RecurrenceRule.Part.BYMONTHDAY, dayOfMonth)
            return this
        }

        fun setEndFor(occurrences: Int): Builder {
            recurrenceRule.count = occurrences
            return this
        }

        fun setEndUntil(endDate: Date): Builder {
            recurrenceRule.until = getFixedDateTime(endDate)
            return this
        }

        fun build(): RecurrenceSetting {
            return RecurrenceSetting(startDate, recurrenceRule)
        }
    }
}

private fun getFreq(type: Int): Freq {
    return when (type) {
        RecurrenceSetting.TYPE_DAILY -> Freq.DAILY
        RecurrenceSetting.TYPE_WEEKLY -> Freq.WEEKLY
        RecurrenceSetting.TYPE_MONTHLY -> Freq.MONTHLY
        RecurrenceSetting.TYPE_YEARLY -> Freq.YEARLY
        else -> Freq.DAILY
    }
}
