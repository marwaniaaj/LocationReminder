package com.udacity.project4.locationreminders.data

import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.lang.Exception

/**
 * Use FakeDataSource that acts as a test double to the LocalDataSource
 */
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private val observableReminders = MutableLiveData<Result<List<ReminderDTO>>>()

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if(shouldReturnError) {
            return Result.Error("Returning testing error!")
        }
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error("Reminders not found!")
//        return Result.Success(remindersServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Returning testing error!")
        }
        reminders?.let {
            return Result.Success(it.single { reminder -> reminder.id == id })
        }
        return Result.Error("Reminder not found!")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    fun addReminder(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
    }
}