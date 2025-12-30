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

    private val isoDateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
    }

    fun parseDate(dateString: String): Date {
        // FAST PATH: Try SQL Format (legacy default) first
        try {
            return sqlDateFormat.get()?.parse(dateString) ?: Date()
        } catch (e: Exception) {
            // Check if it's a Long timestamp (Legacy backup can sometimes use this?)
            // Only try if it looks like a number
            if (dateString.all { it.isDigit() }) {
                try {
                    return Date(dateString.toLong())
                } catch (e: NumberFormatException) {
                    // Ignore
                }
            }
            
            // Fallback to ISO
             try {
                return isoDateFormat.get()?.parse(dateString) ?: Date()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return Date()
    }

    fun formatMonthHeader(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val pattern = if (year == currentYear) {
            "MMMM"
        } else {
            "MMMM, yyyy"
        }
        
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    fun isSameMonth(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}
