package be.robinj.distrohopper.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PreferencesRepositoryTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val repo = PreferencesRepository(this.context)

	@Test
	fun typedAccessorsReadWhatEditWrote() {
		this.repo.edit {
			putInt(Preference.DASHICON_WIDTH.getName(), 42)
			putBoolean(Preference.DEV.getName(), true)
			putString(Preference.THEME.getName(), "gnome")
		}

		assertEquals(42, this.repo.getInt(Preference.DASHICON_WIDTH, 24))
		assertEquals(true, this.repo.getBoolean(Preference.DEV, false))
		assertEquals("gnome", this.repo.getString(Preference.THEME, "default"))
	}

	@Test
	fun accessorsReturnDefaultsWhenUnset() {
		assertEquals(24, this.repo.getInt(Preference.DASHICON_WIDTH, 24))
		assertEquals(false, this.repo.getBoolean(Preference.DEV, false))
		assertEquals("default", this.repo.getString(Preference.THEME, "default"))
	}

	@Test
	fun valueFlowEmitsCurrentValueFirst() = runTest {
		this@PreferencesRepositoryTest.repo.edit {
			putInt(Preference.DASHICON_WIDTH.getName(), 64)
		}

		val first = this@PreferencesRepositoryTest.repo
			.valueFlow(Preference.DASHICON_WIDTH) { it.getInt(Preference.DASHICON_WIDTH.getName(), 24) }
			.first()

		assertEquals(64, first)
	}

	@Test
	fun valueFlowEmitsOnChangeAndCollapsesDuplicates() = runTest {
		val repo = this@PreferencesRepositoryTest.repo
		val values = mutableListOf<Int>()
		val job = launch(UnconfinedTestDispatcher(this.testScheduler)) {
			repo.valueFlow(Preference.DASHICON_WIDTH) {
				it.getInt(Preference.DASHICON_WIDTH.getName(), 24)
			}.take(3).toList(values)
		}

		repo.edit { putInt(Preference.DASHICON_WIDTH.getName(), 24) } // duplicate of default
		repo.edit { putInt(Preference.DASHICON_WIDTH.getName(), 48) }
		repo.edit { putInt(Preference.DASHICON_WIDTH.getName(), 96) }
		job.join()

		assertEquals(listOf(24, 48, 96), values)
	}

	@Test
	fun valueFlowIgnoresOtherKeys() = runTest {
		val repo = this@PreferencesRepositoryTest.repo
		val values = mutableListOf<Int>()
		val job = launch(UnconfinedTestDispatcher(this.testScheduler)) {
			repo.valueFlow(Preference.DASHICON_WIDTH) {
				it.getInt(Preference.DASHICON_WIDTH.getName(), 24)
			}.take(2).toList(values)
		}

		repo.edit { putString(Preference.THEME.getName(), "cinnamon") }
		repo.edit { putInt(Preference.DASHICON_WIDTH.getName(), 48) }
		job.join()

		assertEquals(listOf(24, 48), values)
	}
}
