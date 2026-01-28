package com.smoketracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SmokeRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private val historyPrefs: SharedPreferences = context.getSharedPreferences(
        HISTORY_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val timestampsPrefs: SharedPreferences = context.getSharedPreferences(
        TIMESTAMPS_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun logSmoke() {
        val now = LocalDateTime.now()
        val today = LocalDate.now().format(dateFormatter)
        
        // Get current timestamps list
        val timestamps = getTodayTimestamps().toMutableList()
        timestamps.add(now)
        saveTodayTimestamps(timestamps)
        
        // Save last smoke time
        prefs.edit()
            .putString(KEY_LAST_SMOKE_TIME, now.format(dateTimeFormatter))
            .apply()
        
        // Update today's count
        val savedDate = prefs.getString(KEY_CURRENT_DATE, null)
        val currentCount = if (savedDate == today) {
            prefs.getInt(KEY_SMOKES_TODAY, 0)
        } else {
            // Save previous day's count to history before resetting
            if (savedDate != null) {
                val prevCount = prefs.getInt(KEY_SMOKES_TODAY, 0)
                if (prevCount > 0) {
                    historyPrefs.edit()
                        .putInt(savedDate, prevCount)
                        .apply()
                }
            }
            0
        }
        
        val newCount = currentCount + 1
        prefs.edit()
            .putString(KEY_CURRENT_DATE, today)
            .putInt(KEY_SMOKES_TODAY, newCount)
            .apply()
        
        // Also save to history
        historyPrefs.edit()
            .putInt(today, newCount)
            .apply()
    }
    
    fun decrementSmoke(): LocalDateTime? {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(KEY_CURRENT_DATE, null)
        
        if (savedDate == today) {
            val timestamps = getTodayTimestamps().toMutableList()
            if (timestamps.isNotEmpty()) {
                timestamps.removeAt(timestamps.size - 1)
                saveTodayTimestamps(timestamps)
                
                // Get the new last time (previous smoke if any)
                val newLastTime = timestamps.lastOrNull()
                
                // Only update last smoke time if there are remaining smokes
                // Otherwise keep showing the time of the removed smoke
                if (newLastTime != null) {
                    prefs.edit()
                        .putString(KEY_LAST_SMOKE_TIME, newLastTime.format(dateTimeFormatter))
                        .apply()
                }
                // If no smokes remain, don't clear the last smoke time - keep it for reference
                
                val newCount = timestamps.size
                prefs.edit()
                    .putInt(KEY_SMOKES_TODAY, newCount)
                    .apply()
                
                // Update history
                if (newCount > 0) {
                    historyPrefs.edit()
                        .putInt(today, newCount)
                        .apply()
                } else {
                    historyPrefs.edit()
                        .remove(today)
                        .apply()
                }
                
                return getLastSmokeTime()
            }
        }
        return getLastSmokeTime()
    }
    
    fun getSmokesToday(): Int {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(KEY_CURRENT_DATE, null)
        
        return if (savedDate == today) {
            prefs.getInt(KEY_SMOKES_TODAY, 0)
        } else {
            // Save previous day to history before resetting
            if (savedDate != null) {
                val prevCount = prefs.getInt(KEY_SMOKES_TODAY, 0)
                if (prevCount > 0) {
                    historyPrefs.edit()
                        .putInt(savedDate, prevCount)
                        .apply()
                }
            }
            // Clear timestamps for new day
            timestampsPrefs.edit().clear().apply()
            // Reset counter if it's a new day
            prefs.edit()
                .putString(KEY_CURRENT_DATE, today)
                .putInt(KEY_SMOKES_TODAY, 0)
                .apply()
            0
        }
    }
    
    fun getLastSmokeTime(): LocalDateTime? {
        val timeString = prefs.getString(KEY_LAST_SMOKE_TIME, null) ?: return null
        return try {
            LocalDateTime.parse(timeString, dateTimeFormatter)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getTodayTimestamps(): List<LocalDateTime> {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(KEY_CURRENT_DATE, null)
        
        if (savedDate != today) {
            return emptyList()
        }
        
        val timestampsStr = timestampsPrefs.getString(KEY_TIMESTAMPS, null) ?: return emptyList()
        return timestampsStr.split("|").mapNotNull { str ->
            try {
                LocalDateTime.parse(str, dateTimeFormatter)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun saveTodayTimestamps(timestamps: List<LocalDateTime>) {
        val str = timestamps.joinToString("|") { it.format(dateTimeFormatter) }
        timestampsPrefs.edit()
            .putString(KEY_TIMESTAMPS, str)
            .apply()
    }
    
    fun getHistory(): List<Pair<LocalDate, Int>> {
        val result = mutableListOf<Pair<LocalDate, Int>>()
        
        // Add all history entries
        historyPrefs.all.forEach { (dateStr, count) ->
            try {
                val date = LocalDate.parse(dateStr, dateFormatter)
                val countInt = count as? Int ?: 0
                if (countInt > 0) {
                    result.add(date to countInt)
                }
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }
        
        // Make sure today is included
        val today = LocalDate.now()
        val todayCount = getSmokesToday()
        if (todayCount > 0 && result.none { it.first == today }) {
            result.add(today to todayCount)
        } else if (todayCount > 0) {
            val index = result.indexOfFirst { it.first == today }
            if (index >= 0) {
                result[index] = today to todayCount
            }
        }
        
        return result.sortedByDescending { it.first }
    }
    
    fun getHistoryInRange(startDate: LocalDate, endDate: LocalDate): List<Pair<LocalDate, Int>> {
        return getHistory().filter { (date, _) ->
            !date.isBefore(startDate) && !date.isAfter(endDate)
        }
    }
    
    // Export all data as JSON string
    fun exportData(): String {
        val json = JSONObject()
        
        // Export history
        val historyArray = JSONArray()
        getHistory().forEach { (date, count) ->
            val entry = JSONObject()
            entry.put("date", date.format(dateFormatter))
            entry.put("count", count)
            historyArray.put(entry)
        }
        json.put("history", historyArray)
        
        // Export today's timestamps
        val timestampsArray = JSONArray()
        getTodayTimestamps().forEach { timestamp ->
            timestampsArray.put(timestamp.format(dateTimeFormatter))
        }
        json.put("todayTimestamps", timestampsArray)
        
        // Export last smoke time
        getLastSmokeTime()?.let { lastTime ->
            json.put("lastSmokeTime", lastTime.format(dateTimeFormatter))
        }
        
        // Export current date
        prefs.getString(KEY_CURRENT_DATE, null)?.let { currentDate ->
            json.put("currentDate", currentDate)
        }
        
        // Export smokes today
        json.put("smokesToday", getSmokesToday())
        
        return json.toString(2)
    }
    
    // Import data from JSON string
    fun importData(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            
            // Clear existing data
            historyPrefs.edit().clear().apply()
            timestampsPrefs.edit().clear().apply()
            
            // Import history
            if (json.has("history")) {
                val historyArray = json.getJSONArray("history")
                val editor = historyPrefs.edit()
                for (i in 0 until historyArray.length()) {
                    val entry = historyArray.getJSONObject(i)
                    val date = entry.getString("date")
                    val count = entry.getInt("count")
                    editor.putInt(date, count)
                }
                editor.apply()
            }
            
            // Import today's timestamps
            if (json.has("todayTimestamps")) {
                val timestampsArray = json.getJSONArray("todayTimestamps")
                val timestamps = mutableListOf<LocalDateTime>()
                for (i in 0 until timestampsArray.length()) {
                    val timestamp = LocalDateTime.parse(timestampsArray.getString(i), dateTimeFormatter)
                    timestamps.add(timestamp)
                }
                saveTodayTimestamps(timestamps)
            }
            
            // Import preferences
            val prefsEditor = prefs.edit()
            
            if (json.has("lastSmokeTime")) {
                prefsEditor.putString(KEY_LAST_SMOKE_TIME, json.getString("lastSmokeTime"))
            }
            
            if (json.has("currentDate")) {
                prefsEditor.putString(KEY_CURRENT_DATE, json.getString("currentDate"))
            }
            
            if (json.has("smokesToday")) {
                prefsEditor.putInt(KEY_SMOKES_TODAY, json.getInt("smokesToday"))
            }
            
            prefsEditor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    companion object {
        private const val PREFS_NAME = "smoke_tracker_prefs"
        private const val HISTORY_PREFS_NAME = "smoke_tracker_history"
        private const val TIMESTAMPS_PREFS_NAME = "smoke_tracker_timestamps"
        private const val KEY_LAST_SMOKE_TIME = "last_smoke_time"
        private const val KEY_SMOKES_TODAY = "smokes_today"
        private const val KEY_CURRENT_DATE = "current_date"
        private const val KEY_TIMESTAMPS = "timestamps"
    }
}
