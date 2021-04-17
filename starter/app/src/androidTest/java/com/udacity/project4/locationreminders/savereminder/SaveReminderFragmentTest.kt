package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.MainAndroidTestCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SaveReminderFragmentTest: AutoCloseKoinTest() {

    private lateinit var appContext: Application
    private lateinit var dataSource: ReminderDataSource

    @get: Rule
    val mainCoroutineRule = MainAndroidTestCoroutineRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule
        .grant(android.Manifest.permission.ACCESS_FINE_LOCATION)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // stop the original app koin
        stopKoin()

        appContext = ApplicationProvider.getApplicationContext()
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
     * Test the navigation to [SelectLocationFragment].
     */
    @Test
    fun clickSelectLocation_navigateToSelectLocationFragment() {
        // GIVEN on the save reminder screen
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN click on select location button
        onView(withId(R.id.selectLocation)).perform(click())

        // THEN Verify that we navigate to select location screen
        verify(navController).navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    }

    /**
     * Test the navigation to [ReminderListFragment]
     */
    @Test
    fun clickSaveReminder_navigateToReminderListFragment() {
        // GIVEN on the save reminder screen
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN click on save reminder button
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN Verify that we navigate to reminder list screen
        verify(navController).navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())
    }
}