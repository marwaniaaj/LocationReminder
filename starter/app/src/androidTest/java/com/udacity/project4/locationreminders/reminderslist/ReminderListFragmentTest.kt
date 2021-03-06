package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainAndroidTestCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.After

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {

    private lateinit var appContext: Application
    private lateinit var dataSource: ReminderDataSource

    @get: Rule
    val mainCoroutineRule = MainAndroidTestCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // stop the original app koin
        stopKoin()

        appContext = getApplicationContext()
        var testModules = module {
            viewModel {
                RemindersListViewModel(appContext, get() as ReminderDataSource)
            }
            single {
                SaveReminderViewModel(appContext, get() as ReminderDataSource)
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        // Declare a new koin module
        startKoin {
            modules(listOf(testModules))
        }

        // Get real data source
        dataSource = get()

        // Clear the data to start fresh
        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

    @After
    fun tearDown() {
        runBlocking{
            dataSource.deleteAllReminders()
        }
        stopKoin()
    }

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

    /**
     * Test the navigation to [SaveReminderFragment].
     */
    @Test
    fun clickAddReminderFAB_navigateToSaveReminderFragment() {
        // GIVEN on the reminder list screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN click on "+" button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN Verify that we navigate to save reminder screen
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    /**
     * Test the displayed data on the UI.
     */
    @Test
    fun reminderList_displayRemindersDataInUI() {
        // GIVEN a list of reminders
        runBlocking {
            dataSource.saveReminder(reminder1)
            dataSource.saveReminder(reminder2)
        }

        // WHEN on the reminder list screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN reminders are displayed on the screen
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(hasDescendant(withText(reminder1.title))))
            .check(matches(hasDescendant(withText(reminder1.description))))
            .check(matches(hasDescendant(withText(reminder1.location))))
            .check(matches(hasDescendant(withText(reminder2.title))))
            .check(matches(hasDescendant(withText(reminder2.description))))
            .check(matches(hasDescendant(withText(reminder2.location))))
    }

    /**
     * Add testing for the error messages.
     */
    @Test
    fun remindersList_displayNoData() {
        // GIVEN an empty reminders list
        runBlocking {
            dataSource.deleteAllReminders()
        }

        // WHEN on the reminder list screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN "No Data" is displayed on the screen
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(withText(appContext.getString(R.string.no_data))))
    }
}