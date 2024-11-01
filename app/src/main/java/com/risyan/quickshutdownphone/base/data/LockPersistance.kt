package com.risyan.quickshutdownphone.base.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.content.Context
import android.content.SharedPreferences
import com.risyan.quickshutdownphone.feature.toMinuteAndSecondFormat
import com.google.gson.Gson

@Parcelize
data class LockStatus(
    var startLock: Boolean,
    var endLock: Long
) : Parcelable {
    fun getRemainingDurationTo(
        context: Context
    ): String {
        val value = endLock - System.currentTimeMillis()
        val message = value.toMinuteAndSecondFormat(context)
        return message
    }

}

class SharedPrefApi(context: Context) {

    private val PREFS_NAME = "com.risyan.quickshutdownphone"
    private val LOCK_STATUS_KEY = "lock_status"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLockStatus(lockStatus: LockStatus) {
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(lockStatus)
        editor.putString(LOCK_STATUS_KEY, json)
        editor.apply()
    }

    fun getLockStatus(): LockStatus? {
        val gson = Gson()
        val json = prefs.getString(LOCK_STATUS_KEY, null)
        return gson.fromJson(json, LockStatus::class.java)
    }

    fun removeLockStatus() {
        val editor = prefs.edit()
        editor.remove(LOCK_STATUS_KEY)
        editor.apply()
    }

    fun getCurrentBlankImageCounter(): Int {
        return prefs.getInt("blank_image_counter", 0)
    }

    fun setCurrentBlankImageCounter(counter: Int) {
        val editor = prefs.edit()
        editor.putInt("blank_image_counter", counter)
        editor.apply()
    }
}