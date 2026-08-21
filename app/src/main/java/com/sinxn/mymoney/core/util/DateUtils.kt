package com.sinxn.mymoney.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    
    private val sqlDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        }
    }

    private val simpleDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
    }

    private val isoDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
    }

    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }

    private val monthDayFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM d", Locale.getDefault())
        }
    }

    private val monthDayYearFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        }
    }

    private val parseDateCache = java.util.concurrent.ConcurrentHashMap<String, Date>()
    private val formattedDateCache = java.util.concurrent.ConcurrentHashMap<Long, String>()
    private val monthHeaderCache = java.util.concurrent.ConcurrentHashMap<Long, String>()

    fun getSQLDateTimeString(date: Date): String {
        return sqlDateFormat.get()?.format(date) ?: ""
    }

    fun formatTime(date: Date): String {
        return timeFormat.get()?.format(date) ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    fun formatMonthDay(date: Date): String {
        return monthDayFormat.get()?.format(date) ?: SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }

    fun formatMonthDayYear(date: Date): String {
        return monthDayYearFormat.get()?.format(date) ?: SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }

    private fun fallbackParse(dateString: String): Date {
        try {
            sqlDateFormat.get()?.parse(dateString)?.let { return it }
        } catch (e: Exception) {}

        try {
            simpleDateFormat.get()?.parse(dateString)?.let { return it }
        } catch (e: Exception) {}

        try {
            isoDateFormat.get()?.parse(dateString)?.let { return it }
        } catch (e: Exception) {}

        if (dateString.all { it.isDigit() }) {
            try {
                return Date(dateString.toLong())
            } catch (e: Exception) {}
        }

        return Date()
    }

    fun parseDate(dateString: String): Date {
        if (dateString.isBlank()) return Date()
        parseDateCache[dateString]?.let { return it }

        val trimmed = dateString.trim()
        val len = trimmed.length

        val date = when {
            len == 19 && trimmed[10] == ' ' -> {
                try { sqlDateFormat.get()?.parse(trimmed) ?: fallbackParse(trimmed) }
                catch (e: Exception) { fallbackParse(trimmed) }
            }
            len == 10 && trimmed.getOrNull(4) == '-' -> {
                try { simpleDateFormat.get()?.parse(trimmed) ?: fallbackParse(trimmed) }
                catch (e: Exception) { fallbackParse(trimmed) }
            }
            trimmed.contains('T') -> {
                try { isoDateFormat.get()?.parse(trimmed) ?: fallbackParse(trimmed) }
                catch (e: Exception) { fallbackParse(trimmed) }
            }
            trimmed.all { it.isDigit() } -> {
                try { Date(trimmed.toLong()) }
                catch (e: Exception) { Date() }
            }
            else -> fallbackParse(trimmed)
        }

        parseDateCache[dateString] = date
        return date
    }

    private val DATE_FORMATS = arrayOf(
        "EEEE dd MMMM yyyy",
        "EEEE dd MMM yyyy",
        "EEE dd MMM yyyy",
        "dd MMM yyyy",
        "EEE dd/MM/yyyy",
        "dd/MM/yyyy",
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "EEE MM/dd/yyyy"
    )

    private val cachedDateFormats = object : ThreadLocal<Array<SimpleDateFormat?>>() {
        override fun initialValue(): Array<SimpleDateFormat?> {
            return arrayOfNulls(DATE_FORMATS.size)
        }
    }

    private val monthFormatSameYear = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMMM", Locale.getDefault())
        }
    }

    private val monthFormatDiffYear = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMMM, yyyy", Locale.getDefault())
        }
    }

    fun formatDate(date: Date, index: Int): String {
        val safeIndex = if (index in DATE_FORMATS.indices) index else 2 // Default to medium
        val cacheKey = (date.time shl 4) or (safeIndex.toLong() and 0xF)
        formattedDateCache[cacheKey]?.let { return it }

        val cache = cachedDateFormats.get() ?: return SimpleDateFormat(DATE_FORMATS[safeIndex], Locale.getDefault()).format(date)
        var format = cache[safeIndex]
        if (format == null) {
            format = SimpleDateFormat(DATE_FORMATS[safeIndex], Locale.getDefault())
            cache[safeIndex] = format
        }
        val result = format.format(date)
        formattedDateCache[cacheKey] = result
        return result
    }

    /**
     * Determines the start of the budget month for a given transaction date.
     * E.g. Date=Jan 1, StartDay=15 -> BudgetMonth=Dec 15 (Prev Year)
     * Date=Jan 15, StartDay=15 -> BudgetMonth=Jan 15
     */
    fun getStartOfBudgetMonth(date: Date, firstDayOfMonth: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        
        // Reset time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If current day is before the start day, it belongs to previous month's cycle
        if (currentDay < firstDayOfMonth) {
             cal.add(Calendar.MONTH, -1)
        }
        
        // Ensure day is valid (e.g. Feb 30 -> Feb 28)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = if (firstDayOfMonth > maxDay) maxDay else firstDayOfMonth
        
        cal.set(Calendar.DAY_OF_MONTH, targetDay)
        
        return cal.time
    }

    fun formatMonthHeader(date: Date): String {
        monthHeaderCache[date.time]?.let { return it }

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        
        val result = if (year == currentYear) {
            monthFormatSameYear.get()?.format(date) ?: SimpleDateFormat("MMMM", Locale.getDefault()).format(date)
        } else {
            monthFormatDiffYear.get()?.format(date) ?: SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(date)
        }
        monthHeaderCache[date.time] = result
        return result
    }

    fun isSameMonth(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun isToday(date: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(date: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun addMonths(calendar: Calendar, months: Int): Date {
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, months)
        return cal.time
    }
}
