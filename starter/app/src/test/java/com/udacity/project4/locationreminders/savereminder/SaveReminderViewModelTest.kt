package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import junit.framework.Assert.assertEquals

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake data source to be injected into the view model.
    private lateinit var dataSource: FakeDataSource

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Before
    fun setupViewModel() {
        stopKoin()

        // Initialise the data source with no reminders.
        dataSource = FakeDataSource()

        // Initialize the view model
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            dataSource)
    }

    @Test
    fun onClear_returnNull() {
        // WHEN clear view model
        saveReminderViewModel.onClear()

        // THEN reminder values are null
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }

    @Test
    fun saveNewReminder_newReminderSaved() = mainCoroutineRule.runBlockingTest {
        // GIVEN a reminder item
        val reminder = ReminderDataItem("Title", "Description", "Location", 1.1, 2.2)

        // WHEN request save reminder from view model
        saveReminderViewModel.saveReminder(reminder)

        // THEN show toast, and navigate back
        assertThat(saveReminderViewModel.showToast.value, `is`(saveReminderViewModel.app.getString(R.string.reminder_saved)))
        assertEquals(saveReminderViewModel.navigationCommand.getOrAwaitValue(), NavigationCommand.Back)
    }

    @Test
    fun saveNewReminder_titleIsEmpty_returnFalse_showSnackBar() {
        // GIVEN a reminder item without title
        val reminder = ReminderDataItem("", "Description", "Location", 1.1, 2.2)

        // WHEN validate reminder data
        val validReminder = saveReminderViewModel.validateEnteredData(reminder)

        // THEN
        assertThat(validReminder, `is`(false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun saveNewReminder_locationIsEmpty_returnFalse_showSnackBar() {
        // GIVEN a reminder item without location
        val reminder = ReminderDataItem("Title", "Description", "", 1.1, 2.2)

        // WHEN validate reminder data
        val validReminder = saveReminderViewModel.validateEnteredData(reminder)

        // THEN
        assertThat(validReminder, `is`(false))
        assertThat(saveReminderViewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
    }

    @Test
    fun saveReminder_showLoading() {
        // GIVEN a reminder item
        val reminder = ReminderDataItem("Title", "Description", "Location", 1.1, 2.2)

        // WHEN request save reminder from view model
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.saveReminder(reminder)

        // THEN
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}