package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainAndroidTestCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
/**
 * Add testing implementation to the RemindersLocalRepository.kt
 */
class RemindersLocalRepositoryTest {

    private lateinit var remindersRepository: RemindersLocalRepository
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersDao: RemindersDao

    @get:Rule
    var mainCoroutineRule = MainAndroidTestCoroutineRule()

    private val reminder1 = ReminderDTO(
        "Reminder title 1",
        "Reminder description 1",
        "Reminder location 1",
        24.46017677941061,
        54.42401049833613)

    private val reminder2 = ReminderDTO(
        "Reminder title 2",
        "Reminder description 2",
        "Reminder location 2",
        24.46017677941061,
        54.42401049833613)

    private val reminder3 = ReminderDTO(
        "Reminder title 3",
        "Reminder description 3",
        "Reminder location 3",
        24.46017677941061,
        54.42401049833613)

    private val localReminder = listOf(reminder1, reminder2, reminder3).sortedBy { it.id }

    private val reminderId = UUID.randomUUID().toString()
    private val reminderWithId =  ReminderDTO("Reminder with ID",
        "Reminder Description with ID",
        "Reminder Location with ID",
        24.46017677941061,
        54.42401049833613,
        reminderId)

    private fun saveReminders() = runBlockingTest {
        remindersDao.saveReminder(reminder1)
        remindersDao.saveReminder(reminder2)
        remindersDao.saveReminder(reminder3)
    }

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createRepository() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        remindersDao = remindersDatabase.reminderDao()
        remindersRepository = RemindersLocalRepository(remindersDao, Dispatchers.Main)
    }

    @After
    fun cleanUp() = remindersDatabase.close()

    @Test
    fun getReminders_getAllRemindersFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN a list of three reminders
        saveReminders()

        // WHEN reminders are requested from the reminders repository
        val reminders = (remindersRepository.getReminders() as Result.Success).data.sortedBy { it.id }

        // THEN reminders are loaded from local data source
        assertThat(reminders, notNullValue())
        assertThat(reminders, IsEqual(localReminder))
    }

    @Test
    fun getReminders_getZeroRemindersFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN an empty list of reminders

        // WHEN reminders are requested from the reminders repository
        val reminders = remindersRepository.getReminders() as Result.Success

        // THEN empty reminders list is loaded from local data source
        assertThat(reminders.data.isEmpty(), `is`(true))
    }

    @Test
    fun getReminderById_getReminderByIdFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN a reminder
        remindersDao.saveReminder(reminderWithId)

        // WHEN request reminder by ID from the reminders repository
        val reminder = remindersRepository.getReminder(reminderId) as Result.Success

        // THEN reminder is loaded from local data source
        assertThat(reminder.data, notNullValue())
        assertThat(reminder.data, IsEqual(reminderWithId))
    }

    @Test
    fun getReminderById_idNotFound() = mainCoroutineRule.runBlockingTest {
        // GIVEN a reminder
        remindersDao.saveReminder(reminderWithId)

        // WHEN request reminder by not existing ID from the reminders repository
        val reminder = remindersRepository.getReminder(UUID.randomUUID().toString()) as Result.Error

        // THEN no reminder is loaded from local data source, Error is raised
        assertThat(reminder.message, `is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminder_remindersDeletedFromLocalDataSource() = mainCoroutineRule.runBlockingTest {
        // GIVEN a list of 3 reminders
        saveReminders()

        // WHEN delete all reminders
        remindersDao.deleteAllReminders()
        val reminders = remindersRepository.getReminders() as Result.Success

        // THEN zero reminders are loaded from local data source
        assertThat(reminders.data.isEmpty(), `is`(true))
    }
}