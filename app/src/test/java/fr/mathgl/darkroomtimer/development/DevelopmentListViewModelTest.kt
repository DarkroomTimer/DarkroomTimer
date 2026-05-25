package fr.mathgl.darkroomtimer.development

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import fr.mathgl.darkroomtimer.development.DevelopmentProfileEntity

@OptIn(ExperimentalCoroutinesApi::class)
class DevelopmentListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApplication: Application
    private lateinit var mockDao: DevelopmentDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mock()
        mockDao = mock()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    /** Helper to create a ViewModel with test scope bypassing init block issues */
    private fun createViewModelWithEmptyData(): DevelopmentListViewModel {
        whenever(mockDao.getAllProfiles()).thenReturn(flowOf(emptyList()))
        return DevelopmentListViewModel(mockApplication, mockDao)
    }

    @Test
    fun `initial state has empty profiles list`() = runTest(testDispatcher) {
        val viewModel = createViewModelWithEmptyData()
        runCurrent()

        assertEquals(emptyList<String>(), viewModel.profiles.value.map { it.name })
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `createEmptyProfile returns a profile with empty steps`() {
        val viewModel = createViewModelWithEmptyData()

        val profile = viewModel.createEmptyProfile("New Profile")

        assertEquals("New Profile", profile.name)
        assertEquals(0, profile.stepCount())
        assertEquals(DevelopmentNavigationMode.MANUAL, profile.navigationMode)
    }

    @Test
    fun `createEmptyProfile with AUTOMATIC mode`() {
        val viewModel = createViewModelWithEmptyData()

        val profile = viewModel.createEmptyProfile("Auto Profile", DevelopmentNavigationMode.AUTOMATIC)

        assertEquals("Auto Profile", profile.name)
        assertEquals(DevelopmentNavigationMode.AUTOMATIC, profile.navigationMode)
    }

    @Test
    fun `deleteProfile calls dao deleteProfileById`() = runTest(testDispatcher) {
        val profile = DevelopmentProfile(id = 1L, name = "Test", steps = emptyList())
        val viewModel = createViewModelWithEmptyData()

        viewModel.deleteProfile(profile)
        advanceUntilIdle()

        verify(mockDao).deleteProfileById(1L)
    }

    @Test
    fun `saveProfile with existing id calls dao updateProfile`() = runTest(testDispatcher) {
        val profile = DevelopmentProfile(id = 5L, name = "Existing", steps = emptyList())
        val viewModel = createViewModelWithEmptyData()

        viewModel.saveProfile(profile)
        advanceUntilIdle()

        verify(mockDao).updateProfile(DevelopmentProfileEntity.fromDomain(profile))
    }
}
