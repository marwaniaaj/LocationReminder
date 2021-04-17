package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
/**
 * Provide testing to the RemindersListViewModel and its live data objects
 */
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake data source to be injected into the view model.
    private lateinit var dataSource: FakeDataSource

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        stopKoin()

        // Initialise the data source with no reminders.
        dataSource = FakeDataSource()

        // Initialize the view model
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            dataSource)
    }

    @Test
    fun loadReminders_listNotEmpty() = mainCoroutineRule.runBlockingTest {

        // GIVEN reminders list
        dataSource.deleteAllReminders()
        val reminder = ReminderDTO("Title", "Description", "Location", 1.1, 2.2)
        dataSource.saveReminder(reminder)

        // WHEN request reminders from data source
        remindersListViewModel.loadReminders()

        // THEN reminders are loaded
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().isNotEmpty(), `is`(true))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_returnEmptyList() = mainCoroutineRule.runBlockingTest {
        // GIVEN empty reminders list
        dataSource.deleteAllReminders()

        // WHEN request reminders from data source
        remindersListViewModel.loadReminders()

        // THEN show no data
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().isEmpty(), `is`(true))
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_returnError() = mainCoroutineRule.runBlockingTest {
        // GIVEN empty reminders list
        dataSource.setReturnError(true)

        // WHEN request reminders from data source
        remindersListViewModel.loadReminders()

        // THEN show no data
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Returning testing error!"))
    }

}