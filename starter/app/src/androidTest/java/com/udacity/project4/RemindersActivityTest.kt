package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.delay
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

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        // Stop the original app koin
        stopKoin()

        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        // Declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get our real repository
        repository = get()

        // Clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    @get:Rule
    val activityRule = ActivityTestRule(RemindersActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val backgroundPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    /**
     * add End to End testing to the app
     */
    @Test
    fun reminderActivity_addReminder_endToEnd() {

        // Start up reminders screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify No Data is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        // Click on add reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Type in title and description
        onView(withId(R.id.reminderTitle)).perform(typeText("Reminder title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Reminder description"), closeSoftKeyboard())

        // Click on select location
        onView(withId(R.id.selectLocation)).perform(click())
        runBlocking { delay(1000) }


        // Verify map is displayed
        onView(withId(R.id.map)).check(matches(isDisplayed()))

        // long click on map
        onView(withId(R.id.map)).perform(click())
        runBlocking { delay(1000) }

        // Click on save button
        onView(withId(R.id.save_location_button)).perform(click())
        runBlocking { delay(1000) }

        // Click on save reminder button
        onView(withId(R.id.saveReminder)).perform(click())

        // Verify reminder is displayed on screen in the task list.
        onView(withText("Reminder title")).check(matches(isDisplayed()))
        onView(withText("Reminder description")).check(matches(isDisplayed()))

        // Verify No data is not displayed
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }
}
