package be.robinj.distrohopper.home

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class DefaultPinnedAppsTest {
	private lateinit var application: Application

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
	}

	private fun app(packageName: String): App = App(
		this.application,
		null,
		ActivityTestSupport.resolveInfo(packageName, "MainActivity", packageName),
	)

	private fun install(packageName: String, system: Boolean = false) {
		val applicationInfo = ApplicationInfo().apply {
			this.packageName = packageName
			if (system) flags = flags or ApplicationInfo.FLAG_SYSTEM
		}
		Shadows.shadowOf(this.application.packageManager).installPackage(
			PackageInfo().apply {
				this.packageName = packageName
				this.applicationInfo = applicationInfo
			},
		)
	}

	@Test fun selectsOneAppPerCategoryInPriorityOrder() {
		val installed = listOf(
			app("com.brave.browser"),
			app("com.android.chrome"),
			app("org.mozilla.firefox"),
			app("com.microsoft.office.outlook"),
			app("com.google.android.gm"),
			app("com.android.camera2"),
		)
		installed.forEach { install(it.packageName) }

		assertEquals(
			listOf("org.mozilla.firefox", "com.google.android.gm", "com.android.camera2"),
			DefaultPinnedApps.select(installed, emptyList(), this.application.packageManager)
				.map { it.packageName },
		)
	}

	@Test fun skipsMissingCategoriesAndCategoriesAlreadyPinned() {
		val firefox = app("org.mozilla.firefox")
		val outlook = app("com.microsoft.office.outlook")
		install(firefox.packageName)
		install(outlook.packageName)

		assertEquals(
			listOf(outlook),
			DefaultPinnedApps.select(
				listOf(firefox, outlook),
				listOf(firefox),
				this.application.packageManager,
			),
		)
	}

	@Test fun knownCameraAppPrefersASystemPackage() {
		val googleCamera = app("com.google.android.GoogleCamera")
		val aospCamera = app("com.android.camera2")
		install(googleCamera.packageName)
		install(aospCamera.packageName, system = true)

		assertEquals(
			aospCamera,
			DefaultPinnedApps.select(
				listOf(googleCamera, aospCamera),
				emptyList(),
				this.application.packageManager,
			).single(),
		)
	}

	@Test fun knownCameraAppFallsBackToPackagePriorityWhenNoneIsASystemApp() {
		val googleCamera = app("com.google.android.GoogleCamera")
		val aospCamera = app("com.android.camera2")
		install(googleCamera.packageName)
		install(aospCamera.packageName)

		assertEquals(
			googleCamera,
			DefaultPinnedApps.select(
				listOf(aospCamera, googleCamera),
				emptyList(),
				this.application.packageManager,
			).single(),
		)
	}
}
