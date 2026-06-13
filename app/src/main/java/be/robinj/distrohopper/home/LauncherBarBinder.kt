package be.robinj.distrohopper.home

import android.os.PowerManager
import android.os.UserHandle
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.desktop.dash.ProfilePagerAdapter
import be.robinj.distrohopper.desktop.dash.profile.GnomeProfilePillIndicator
import be.robinj.distrohopper.desktop.dash.profile.UnityRibbonIndicator
import be.robinj.distrohopper.desktop.dash.profile.ProfileIndicator
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.AppLauncherClickListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherDragListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherLongClickListener
import be.robinj.distrohopper.desktop.launcher.PinnedAppsBar
import be.robinj.distrohopper.desktop.launcher.RunningAppLauncher
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.ProfileIndicatorStyle

/**
 * The view half of app management: keeps the launcher bar's pinned and
 * running app icons and the dash grid in sync with the model. Split out of
 * AppManager, which remains the facade gluing this to AppRepository. Views
 * are resolved lazily so AppManager can still be constructed on a
 * background thread during startup loading.
 */
class LauncherBarBinder(private val appManager: AppManager) {
	private val activity = this.appManager.parent

	/** While a pinned icon is dragged, its own view stays in the bar as an
	 *  invisible placeholder: the empty slot previewing where it would drop. */
	private var draggedPinnedApp: AppLauncher? = null
	private var draggedPinnedAppOldIndex = -1
	private var draggedPinnedAppDropped = false

	/** The desktops the in-flight launcher morph is between (-1 = not morphing). */
	private var morphFrom = -1
	private var morphTo = -1
	private var morphStride = 0F
	private var morphVertical = true

	private val llLauncher: LinearLayout by lazy {
		this.activity.viewFinder.get(R.id.llLauncher)
	}
	private val llLauncherPinnedApps: PinnedAppsBar by lazy {
		this.activity.viewFinder.get(this.llLauncher, R.id.llLauncherPinnedApps)
	}
	private val llLauncherRunningApps: LinearLayout by lazy {
		this.activity.viewFinder.get(this.llLauncher, R.id.llLauncherRunningApps)
	}
	private val vpDashProfiles: ViewPager2 by lazy {
		this.activity.viewFinder.get(R.id.vpDashProfiles)
	}

	private var dashBound = false
	/** The profiles the dash was last bound for (null = the personal profile). */
	private var boundProfiles: List<UserHandle?> = emptyList()
	/** The current profile tab; preserved across rebinds (app install/remove). */
	private var currentProfileIndex = 0
	private var pagerAdapter: ProfilePagerAdapter? = null
	private var indicator: ProfileIndicator? = null
	private var pageCallbackRegistered = false
	private var dashOpen = false

	fun addPinnedAppView(app: App) {
		this.llLauncherPinnedApps.addView(this.pinnedAppLauncher(app))
	}

	fun refreshPinnedView() {
		// A plain rebuild ends any in-flight morph. LayoutTransition is suppressed
		// so the whole bar doesn't fade its icons in one by one (the "flash") //
		this.morphFrom = -1
		this.morphTo = -1
		this.llLauncherPinnedApps.clearMorph()
		this.withPinnedLayoutTransitionSuppressed {
			this.llLauncherPinnedApps.removeAllViews()
			for (app in this.appManager.pinned) {
				this.llLauncherPinnedApps.addView(this.pinnedAppLauncher(app))
			}
		}
	}

	fun removePinnedAppView(app: App) {
		val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
		this.llLauncherPinnedApps.removeView(appLauncher)
	}

	//# Per-desktop launcher morph (driven by WidgetsPager's scroll) #//

	/** Settles the launcher on [page]: rebuild its plain bar, ending any morph. */
	fun showDesktop(page: Int) {
		if (! this.appManager.isPerDesktopPins) {
			return // Global pins: the bar is identical on every desktop, never rebuild //
		}

		val previous = this.appManager.currentDesktop
		this.appManager.setCurrentDesktop(page)
		// Only rebuild if a morph was in flight or the desktop actually changed //
		if (this.morphFrom != -1 || previous != this.appManager.currentDesktop) {
			this.refreshPinnedView()
		}
	}

	/**
	 * Tracks a swipe between desktops [fromPage] and [toPage] at [fraction]: the
	 * pinned icons slide/fade between the two desktops' layouts, the bar resizing
	 * with them. A no-op in global mode (the bar is identical on every desktop)
	 * and in battery saver (the bar just swaps when the swipe settles).
	 */
	fun onPageScroll(fromPage: Int, toPage: Int, fraction: Float) {
		if (! this.appManager.isPerDesktopPins || fraction <= 0F || ! this.animationsEnabled()) {
			return
		}

		if (fromPage != this.morphFrom || toPage != this.morphTo) {
			this.buildMorph(fromPage, toPage)
		}
		this.applyMorph(fraction)
	}

	private fun buildMorph(fromPage: Int, toPage: Int) {
		this.morphVertical = this.llLauncherPinnedApps.orientation == LinearLayout.VERTICAL
		this.morphStride = this.captureStride() // Read from the current (from) bar first //
		this.morphFrom = fromPage
		this.morphTo = toPage

		// Build the union once, without the LayoutTransition fading each icon in //
		val union = LauncherMorph.union(
			this.appManager.pinnedOn(fromPage), this.appManager.pinnedOn(toPage))
		this.withPinnedLayoutTransitionSuppressed {
			this.llLauncherPinnedApps.removeAllViews()
			for (app in union) {
				this.llLauncherPinnedApps.addView(this.pinnedAppLauncher(app))
			}
		}
	}

	private fun applyMorph(fraction: Float) {
		val from = this.appManager.pinnedOn(this.morphFrom)
		val to = this.appManager.pinnedOn(this.morphTo)
		// The bar's length interpolates between the two desktops' icon counts, so
		// an auto-sizing launcher resizes smoothly with the morph //
		val length = from.size + (to.size - from.size) * fraction

		this.llLauncherPinnedApps.setMorph(
			LauncherMorph.slots(from, to, fraction), this.morphStride, length)
	}

	/** The per-slot advance along the bar's axis: a laid-out icon's size, else computed. */
	private fun captureStride(): Float {
		val bar = this.llLauncherPinnedApps
		if (bar.childCount > 0) {
			val child = bar.getChildAt(0)
			val size = if (this.morphVertical) child.height else child.width
			if (size > 0) {
				return size.toFloat()
			}
		}

		val density = this.activity.resources.displayMetrics.density
		val iconWidth = Preferences.getSharedPreferences(this.activity)
			.getInt(Preference.LAUNCHERICON_WIDTH.getName(), 36)
		val width = (48 + iconWidth) * density
		return if (this.morphVertical) width - 4F * density else width
	}

	private fun animationsEnabled(): Boolean =
		this.activity.getSystemService(PowerManager::class.java)?.isPowerSaveMode != true

	/* Mutating the bar's children fires its LayoutTransition (appear animations); suppress it. */
	private fun withPinnedLayoutTransitionSuppressed(block: () -> Unit) {
		val bar = this.llLauncherPinnedApps
		val saved = bar.layoutTransition
		bar.layoutTransition = null
		try {
			block()
		} finally {
			bar.layoutTransition = saved
		}
	}

	fun addRunningApps(colour: Int) {
		var colour = colour
		this.llLauncherRunningApps.removeAllViews()

		for (i in 0 until this.llLauncherPinnedApps.childCount)
			(this.llLauncherPinnedApps.getChildAt(i) as AppLauncher).setRunning(false)

		for (app in this.appManager.runningApps) {
			if (this.appManager.isPinned(app)) {
				val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
				appLauncher?.setRunning(true)
			} else {
				val theme = DependencyContainer.of(this.activity).themeManager.current
				if (! this.activity.resources.getBoolean(theme.launcher_applauncher_backgroundcolour_dynamic))
					colour = this.activity.resources.getColor(theme.launcher_applauncher_backgroundcolour)

				val appLauncher = RunningAppLauncher(this.activity, app)
				appLauncher.setOnClickListener(AppLauncherClickListener(this.activity))
				appLauncher.colour = colour

				this.llLauncherRunningApps.addView(appLauncher)
			}
		}
	}

	/**
	 * Binds the dash app grid: always a ViewPager2 with one swipeable page per
	 * profile — a single page in the usual single-profile case, so the dash
	 * looks and behaves like the plain grid. A theme-specific tab indicator is
	 * shown only when more than one profile exists.
	 */
	fun bindDashApps() {
		this.dashBound = true

		this.rebindDashApps()
	}

	private fun rebindDashApps() {
		val profiles = this.appManager.repository.profiles()
		this.boundProfiles = profiles

		val selected = this.currentProfileIndex.coerceIn(0, profiles.size - 1)
		this.currentProfileIndex = selected

		val adapter = ProfilePagerAdapter(this.activity, this.appManager, profiles)
		this.pagerAdapter = adapter
		this.vpDashProfiles.adapter = adapter
		this.vpDashProfiles.setCurrentItem(selected, false)
		this.registerPageCallback()

		// The tab indicator only appears once there is more than one profile //
		this.indicator?.clear()
		this.indicator = if (profiles.size > 1) {
			this.createIndicator()?.also {
				it.bind(profiles, selected)
				it.onDashOpenChanged(this.dashOpen)
			}
		} else {
			null
		}
	}

	private fun registerPageCallback() {
		if (this.pageCallbackRegistered) {
			return
		}
		this.pageCallbackRegistered = true

		this.vpDashProfiles.registerOnPageChangeCallback(
			object : ViewPager2.OnPageChangeCallback() {
				override fun onPageScrolled(
					position: Int, positionOffset: Float, positionOffsetPixels: Int) {
					this@LauncherBarBinder.indicator?.onPageScrolled(position, positionOffset)
				}

				override fun onPageSelected(position: Int) {
					this@LauncherBarBinder.currentProfileIndex = position
					this@LauncherBarBinder.indicator?.onPageSelected(position)
				}
			})
	}

	private fun createIndicator(): ProfileIndicator? {
		val theme = DependencyContainer.of(this.activity).themeManager.current
		val select: (Int) -> Unit = { this.vpDashProfiles.setCurrentItem(it, true) }

		return when (ProfileIndicatorStyle.of(
				this.activity.resources.getInteger(theme.profile_indicator))) {
			ProfileIndicatorStyle.UNITY_RIBBON -> UnityRibbonIndicator(this.activity,
				this.activity.viewFinder.get(R.id.llDashRibbonProfiles),
				theme.profile_indicator_personal_glyph, select)
			ProfileIndicatorStyle.GNOME_PANEL -> GnomeProfilePillIndicator(this.activity,
				this.activity.viewFinder.get(R.id.llPanelProfileIndicator), select)
			ProfileIndicatorStyle.NONE -> null
		}
	}

	fun notifyDashAdapterChanged() {
		if (this.dashBound && this.appManager.repository.profiles() != this.boundProfiles) {
			// A profile appeared or vanished (e.g. the first work-profile app was
			// installed, or the last removed): rebuild the pager and indicator //
			this.rebindDashApps()

			return
		}

		this.pagerAdapter?.refresh()
	}

	fun invalidateDashViews() {
		this.pagerAdapter?.invalidatePages(this.vpDashProfiles)
	}

	/** Re-applies the dash grid's column count to the pager pages (pref/rotation change). */
	fun applyDashColumns() {
		this.pagerAdapter?.applyColumns(this.vpDashProfiles)
	}

	/** The dash opened or closed; indicators that only show while open react. */
	fun setDashOpen(open: Boolean) {
		this.dashOpen = open
		this.indicator?.onDashOpenChanged(open)
	}

	fun startedDraggingPinnedApp() = startedDragging(this.activity)

	fun startedDraggingPinnedApp(app: App) {
		val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
		if (appLauncher != null) {
			this.draggedPinnedApp = appLauncher
			this.draggedPinnedAppOldIndex = this.llLauncherPinnedApps.indexOfChild(appLauncher)
			this.draggedPinnedAppDropped = false
			appLauncher.visibility = View.INVISIBLE
		}

		this.startedDraggingPinnedApp()
	}

	/**
	 * A not-yet-pinned app dragged from the dash: a placeholder slot opens
	 * at the end of the bar and follows the drag like a reorder; the app is
	 * only actually pinned if it is dropped on the bar.
	 */
	fun startedDraggingDashApp(app: App) {
		val appLauncher = this.pinnedAppLauncher(app)
		appLauncher.visibility = View.INVISIBLE
		this.llLauncherPinnedApps.addView(appLauncher)

		this.draggedPinnedApp = appLauncher
		this.draggedPinnedAppOldIndex = NOT_YET_PINNED
		this.draggedPinnedAppDropped = false

		this.startedDraggingPinnedApp()
	}

	/**
	 * The drag is hovering over another pinned icon: shift the placeholder
	 * into that icon's slot, so that the icons in between slide over
	 * (animated by the container's LayoutTransition) and the empty slot
	 * shows exactly where the dragged icon would land.
	 */
	fun draggedPinnedAppOver(target: App) {
		val dragged = this.draggedPinnedApp ?: return
		val targetView = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(target)
		if (targetView == null || targetView == dragged)
			return

		val targetIndex = this.llLauncherPinnedApps.indexOfChild(targetView)
		if (targetIndex < 0)
			return

		this.llLauncherPinnedApps.removeView(dragged)
		this.llLauncherPinnedApps.addView(dragged, targetIndex)
	}

	/** Commits the order previewed by the placeholder's position. */
	fun droppedPinnedApp() {
		val dragged = this.draggedPinnedApp ?: return
		val newIndex = this.llLauncherPinnedApps.indexOfChild(dragged)
		if (newIndex < 0)
			return
		this.draggedPinnedAppDropped = true

		if (this.draggedPinnedAppOldIndex == NOT_YET_PINNED) {
			// A dash app dropped onto the bar: pin appends it to the model,
			// then shift it to the previewed slot //
			if (!this.appManager.pin(dragged.tag as App, false, false, false)) {
				this.draggedPinnedAppDropped = false // pinned meanwhile: let ended rebuild //
				return
			}

			val appendedIndex = this.appManager.pinned.size - 1
			if (newIndex != appendedIndex)
				this.appManager.movePinnedApp(appendedIndex, newIndex)
			this.appManager.savePinnedApps()
		} else if (newIndex != this.draggedPinnedAppOldIndex) {
			this.appManager.movePinnedApp(this.draggedPinnedAppOldIndex, newIndex)
			this.appManager.savePinnedApps()
		}
	}

	fun endedDraggingPinnedApp() {
		val dragged = this.draggedPinnedApp ?: return
		this.draggedPinnedApp = null
		this.draggedPinnedAppOldIndex = -1

		dragged.visibility = View.VISIBLE
		if (!this.draggedPinnedAppDropped)
			this.refreshPinnedView() // Cancelled, or unpinned via the trash: snap back to the model's order //
	}

	fun stoppedDraggingPinnedApp() = stoppedDragging(this.activity)

	private fun pinnedAppLauncher(app: App): AppLauncher {
		val appLauncher = AppLauncher(this.activity, app)
		appLauncher.setOnClickListener(AppLauncherClickListener(this.activity))
		appLauncher.setOnLongClickListener(AppLauncherLongClickListener(this.activity))
		appLauncher.setOnDragListener(AppLauncherDragListener(this.appManager))

		return appLauncher
	}

	companion object {
		/** draggedPinnedAppOldIndex value for a dash app not yet in the pinned model. */
		private const val NOT_YET_PINNED = -1

		/*
		 * The drag decorations only touch views, not the app model, and widget
		 * drags can start before app loading has finished — so these are usable
		 * with just the activity.
		 */

		@JvmStatic
		fun startedDragging(activity: HomeActivity) {
			val viewFinder = activity.viewFinder
			val llLauncher = viewFinder.get<LinearLayout>(R.id.llLauncher)
			val lalBfb = viewFinder.get<AppLauncher>(llLauncher, R.id.lalBfb)
			val lalPreferences = viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
			val lalTrash = viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)

			val theme = DependencyContainer.of(activity).themeManager.current
			if (activity.resources.getBoolean(theme.launcher_bfb_hide_while_dragging)) {
				lalBfb.visibility = View.GONE
			}
			lalPreferences.visibility = View.GONE
			lalTrash.visibility = View.VISIBLE
			activity.closeDash()

			viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherPinnedApps).alpha = 0.9F
		}

		@JvmStatic
		fun stoppedDragging(activity: HomeActivity) {
			val viewFinder = activity.viewFinder
			val llLauncher = viewFinder.get<LinearLayout>(R.id.llLauncher)
			val lalBfb = viewFinder.get<AppLauncher>(llLauncher, R.id.lalBfb)
			val lalPreferences = viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
			val lalTrash = viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)

			val theme = DependencyContainer.of(activity).themeManager.current
			val lalPreferences_location = theme.lalPreferences_getLocation(
				activity.resources, Preferences.getSharedPreferences(activity))
			lalBfb.visibility = View.VISIBLE
			lalPreferences.visibility =
				if (lalPreferences_location == Location.NONE) View.GONE else View.VISIBLE
			lalTrash.visibility = View.GONE

			viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherPinnedApps).alpha = 1.0F
		}
	}
}
