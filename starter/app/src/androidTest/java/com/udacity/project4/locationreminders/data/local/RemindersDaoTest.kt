package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Test
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
/**
 * Add testing implementation to the RemindersDao.kt
 */
class RemindersDaoTest {

    private lateinit var remindersDao: RemindersDao
    private lateinit var reminderDatabase: RemindersDatabase

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

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupDatabase() {
        reminderDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        remindersDao = reminderDatabase.reminderDao()
    }

    @After
    fun cleanUp() = reminderDatabase.close()

    private fun saveReminders() = runBlockingTest {
        remindersDao.saveReminder(reminder1)
        remindersDao.saveReminder(reminder2)
        remindersDao.saveReminder(reminder3)
    }

    @Test
    fun getReminders_getAllRemindersFromDatabase() = runBlockingTest {
        // GIVEN a list of 3 reminders
        saveReminders()

        // WHEN reminders are requested from database
        var reminders = remindersDao.getReminders().sortedBy { it.id }

        // THEN reminders are loaded from database
        assertThat(reminders.count(), `is`(3))
        assertThat(reminders, IsEqual(localReminder))
    }

    @Test
    fun getReminders_getZeroRemindersFromDatabase() = runBlockingTest {
        // GIVEN an empty list of reminders
        remindersDao.deleteAllReminders()

        // WHEN reminders are requested from database
        var reminders = remindersDao.getReminders()

        // THEN empty reminders list is loaded from database
        assertThat(reminders.isEmpty(), `is`(true))
    }

    @Test
    fun getReminderById_getReminderByIdFromDatabase() = runBlockingTest {
        // GIVEN a reminder
        remindersDao.saveReminder(reminder1)

        // WHEN request reminder by ID from database
        val reminder = remindersDao.getReminderById(reminder1.id)

        //THEN reminder is loaded from database
        assertThat<ReminderDTO>(reminder as ReminderDTO, notNullValue())
        assertThat(reminder.id, `is`(reminder1.id))
    }

    @Test
    fun getReminderById_idNotFound() = runBlockingTest {
        // GIVEN a reminder
        remindersDao.saveReminder(reminder1)

        // WHEN request reminder by not existing ID from database
        val reminder = remindersDao.getReminderById(UUID.randomUUID().toString())

        //THEN "no" reminder is loaded from database
        assertThat(reminder, `is`(nullValue()))
    }

    @Test
    fun deleteAllReminder_remindersDeletedFromDatabase() = runBlockingTest {
        // GIVEN a list of 3 reminders, delete them
        saveReminders()
        remindersDao.deleteAllReminders()

        // WHEN request reminders from database
        val reminders = remindersDao.getReminders()

        // THEN empty reminders list is loaded from database
        assertThat(reminders.isEmpty(), `is`(true))
    }

}