package be.robinj.distrohopper.home

import android.animation.LayoutTransition
import android.os.PowerManager
import android.os.UserHandle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.desktop.dash.FolderPopup
import be.robinj.distrohopper.desktop.dash.ProfilePagerAdapter
import be.robinj.distrohopper.desktop.dash.profile.GnomeProfilePillIndicator
import be.robinj.distrohopper.desktop.dash.profile.UnityRibbonIndicator
import be.robinj.distrohopper.desktop.dash.profile.ProfileIndicator
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.AppLauncherClickListener
import be.robinj.distrohopper.desktop.launcher.AppLauncherLongClickListener
import be.robinj.distrohopper.desktop.launcher.LauncherDragPayload
import be.robinj.distrohopper.desktop.launcher.LauncherFolderView
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.desktop.launcher.LauncherItem
import be.robinj.distrohopper.desktop.launcher.PinnedAppsBar
import be.robinj.distrohopper.desktop.launcher.RunningAppLauncher
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.theme.ProfileIndicatorStyle
import be.robinj.distrohopper.widgets.WidgetsPager

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
	/** The view (app icon or folder) being dragged within the bar; its empty slot previews the drop. */
	private var draggedPinnedApp: View? = null
	private var draggedPinnedAppOldIndex = -1
	private var draggedPinnedAppDropped = false
	/** When the drag is an app extracted from a launcher folder: the source folder
	 *  id, so a committing drop ungroups it (left null for an ordinary pin drag). */
	private var extractFromFolderId: String? = null

	/** The icon/folder a dwell has armed for a fold-on-drop (held centrally; see [hoverPinnedItem]). */
	private var foldArmedTarget: View? = null
	private val foldHandler = android.os.Handler(android.os.Looper.getMainLooper())

	/** The desktops the in-flight launcher morph is between (-1 = not morphing). */
	private var morphFrom = -1
	private var morphTo = -1
	private var morphStride = 0F
	private var morphVertical = true
	/** Container transition saved while morphing so it is not fired per-frame. */
	private var savedContainerTransition: LayoutTransition? = null

	private val llLauncherAndDashContainer: LinearLayout by lazy {
		this.activity.viewFinder.get(R.id.llLauncherAndDashContainer)
	}
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
	/** Last laid-out size of the apps grid viewport; 0 until first measured. */
	private var dashGridViewportWidthPx = 0
	private var dashGridViewportHeightPx = 0

	fun addPinnedAppView(app: App) {
		this.llLauncherPinnedApps.addView(this.pinnedAppLauncher(app))
	}

	fun refreshPinnedView() {
		val wasMorphing = this.morphFrom != -1
		// A plain rebuild ends any in-flight morph. LayoutTransition is suppressed
		// so the whole bar doesn't fade its icons in one by one (the "flash") //
		this.morphFrom = -1
		this.morphTo = -1
		this.llLauncherPinnedApps.clearMorph()
		// Restore the container transition suppressed during the morph so the
		// dock's final resize can animate cleanly with a single CHANGING pass. //
		if (wasMorphing) {
			this.llLauncherAndDashContainer.layoutTransition = this.savedContainerTransition
			this.savedContainerTransition = null
		}
		this.withPinnedLayoutTransitionSuppressed {
			this.llLauncherPinnedApps.removeAllViews()
			for (item in this.appManager.launcherLayout.launcherItems(this.appManager.currentDesktop)) {
				this.llLauncherPinnedApps.addView(this.launcherItemView(item))
			}
		}
	}

	/** A pinned-bar child for a launcher item: an app icon or a folder mini-grid. */
	private fun launcherItemView(item: LauncherItem): View = when (item) {
		is LauncherItem.LauncherApp -> this.pinnedAppLauncher(item.app)
		is LauncherItem.LauncherFolder -> this.launcherFolderView(item)
	}

	private fun launcherFolderView(item: LauncherItem.LauncherFolder): View {
		val view = LauncherFolderView(this.activity, item.folder.id, item.apps)
		view.setOnClickListener {
			FolderPopup(this.activity, item.folder.id, item.apps,
				clipLabel = "launcherFolderMember",
				memberPayload = { app -> LauncherDragPayload.FolderMemberDrag(item.folder.id, app) })
				.showAt(view)
		}
		view.setOnLongClickListener {
			AppLauncherLongClickListener.startFolderDrag(this.activity, view, item.folder.id)
			true
		}
		// Reorder/fold is hit-tested at the container level by LauncherDragListener
		// (nested icons don't receive drag LOCATION events), so no per-icon listener.

		return view
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

		// Suppress the container's CHANGING transition for the morph's lifetime:
		// applyMorph calls requestLayout every frame via setMorph, which changes
		// the dock's measured size each frame. Each change would fire-then-cancel
		// a CHANGING animation, creating a noisy cancel-restart cycle. The
		// transition is restored in refreshPinnedView so the final settle resize
		// runs as one clean animation. //
		if (this.savedContainerTransition == null) {
			this.savedContainerTransition = this.llLauncherAndDashContainer.layoutTransition
			this.llLauncherAndDashContainer.layoutTransition = null
		}

		// Build the union once, without the LayoutTransition fading each icon in //
		val union = LauncherMorph.union(
			this.appManager.launcherLayout.launcherItems(fromPage),
			this.appManager.launcherLayout.launcherItems(toPage))
		this.withPinnedLayoutTransitionSuppressed {
			this.llLauncherPinnedApps.removeAllViews()
			for (item in union) {
				this.llLauncherPinnedApps.addView(this.launcherItemView(item))
			}
		}
	}

	private fun applyMorph(fraction: Float) {
		val from = this.appManager.launcherLayout.launcherItems(this.morphFrom)
		val to = this.appManager.launcherLayout.launcherItems(this.morphTo)
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

		return if (this.morphVertical) LauncherIconGrid.iconHeightPx(this.activity).toFloat()
			else LauncherIconGrid.iconSizePx(this.activity).toFloat()
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
			(this.llLauncherPinnedApps.getChildAt(i) as? AppLauncher)?.setRunning(false)

		for (app in this.appManager.runningApps) {
			if (this.appManager.isPinned(app)) {
				// Folder members have no loose icon to flag as running; skip them.
				val appLauncher = this.llLauncherPinnedApps.findViewWithTag<AppLauncher>(app)
				appLauncher?.setRunning(true)
			} else {
				val theme = DependencyContainer.of(this.activity).themeManager.current
				if (! this.activity.resources.getBoolean(theme.launcher_applauncher_backgroundcolour_dynamic))
					colour = this.activity.resources.getColor(theme.launcher_applauncher_backgroundcolour, null)

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

		// Remember the apps grid's real viewport whenever it is laid out, so the
		// customise-mode grid-size hint can show the true rows for the current
		// theme and orientation (the apps grid is GONE while customising) //
		this.vpDashProfiles.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
			val w = r - l
			val h = b - t
			if (w > 0 && h > 0) {
				this.dashGridViewportWidthPx = w
				this.dashGridViewportHeightPx = h
			}
		}

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

	/**
	 * The apps grid's last laid-out viewport (width, height) in px, or null if
	 * it has not been measured yet. Used by the customise-mode hint to show the
	 * real row count for the current theme and orientation.
	 */
	fun dashGridViewport(): Pair<Int, Int>? =
		if (this.dashGridViewportWidthPx > 0 && this.dashGridViewportHeightPx > 0) {
			this.dashGridViewportWidthPx to this.dashGridViewportHeightPx
		} else {
			null
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
	 * Begins dragging an app pulled out of a launcher folder, so it behaves exactly
	 * like dragging the pin itself: a placeholder opens in the bar and follows the
	 * drag (gaps to reposition, fold onto an icon to make a folder, trash to remove).
	 *
	 * A folder only *groups* already-pinned apps, so — unlike a dash app — the app
	 * is NOT pinned on drop; instead a committing drop ungroups it from [folderId]
	 * (see [droppedPinnedApp] / [foldDraggedOnto]). The model is untouched until
	 * then, so cancelling simply leaves the app in its folder (and avoids dissolving
	 * a two-member source folder the instant the drag starts).
	 */
	fun startedDraggingLauncherFolderMember(folderId: String, app: App) {
		if (this.draggedPinnedApp != null) {
			return // a placeholder already exists for this drag //
		}
		this.extractFromFolderId = folderId
		val appLauncher = this.pinnedAppLauncher(app)
		appLauncher.visibility = View.INVISIBLE
		this.llLauncherPinnedApps.addView(appLauncher)

		this.draggedPinnedApp = appLauncher
		this.draggedPinnedAppOldIndex = EXTRACTED_FOLDER_MEMBER
		this.draggedPinnedAppDropped = false

		this.startedDraggingPinnedApp()
	}

	/**
	 * The drag is hovering over another pinned item: shift the placeholder into
	 * [targetView]'s slot, so the items in between slide over (animated by the
	 * container's LayoutTransition) and the empty slot shows where the drop lands.
	 */
	fun draggedPinnedItemOver(targetView: View) {
		val dragged = this.draggedPinnedApp ?: return
		if (targetView == dragged)
			return

		val targetIndex = this.llLauncherPinnedApps.indexOfChild(targetView)
		if (targetIndex < 0)
			return

		this.llLauncherPinnedApps.removeView(dragged)
		this.llLauncherPinnedApps.addView(dragged, targetIndex)
	}

	/** Compat shim: the per-icon drag listener and tests hover by target app. */
	fun draggedPinnedAppOver(target: App) {
		val targetView = this.llLauncherPinnedApps.findViewWithTag<View>(target) ?: return
		this.draggedPinnedItemOver(targetView)
	}

	/**
	 * Whether the dragged item could fold onto [targetView] (the dragged item is
	 * an app, and the target is a different app or a folder). Folders being
	 * dragged never fold.
	 */
	fun canFoldOnto(targetView: View): Boolean {
		val dragged = this.draggedPinnedApp ?: return false
		if (targetView === dragged) {
			return false
		}
		val draggedApp = dragged.tag as? App ?: return false
		val targetTag = targetView.tag

		return targetView is LauncherFolderView || (targetTag is App && targetTag != draggedApp)
	}

	/**
	 * Preview an insertion (reorder / pin) at the gap before/after [targetView]
	 * (or the end of the bar when null): the dragged item's empty slot opens
	 * there, so dropping pins to that location. Clears any fold preview — an open
	 * gap means "drop to pin here", never a fold.
	 */
	fun previewPinnedInsert(targetView: View?, after: Boolean) {
		val dragged = this.draggedPinnedApp ?: return
		this.setFoldRing(null)

		val bar = this.llLauncherPinnedApps
		bar.removeView(dragged)
		val index = if (targetView == null || targetView === dragged) {
			bar.childCount
		} else {
			bar.indexOfChild(targetView) + if (after) 1 else 0
		}
		bar.addView(dragged, index.coerceIn(0, bar.childCount))
	}

	/**
	 * Preview a fold onto [targetView] (the drag is over its centre): ring the
	 * target so dropping creates/extends a folder. The dragged item's placeholder
	 * is left exactly where it is (the insertion preview "freezes"), so moving
	 * between a gap and an icon's centre only toggles the ring — it never jumps
	 * the icons around.
	 */
	fun previewPinnedFold(targetView: View) {
		val dragged = this.draggedPinnedApp ?: return
		if (targetView === dragged) {
			return
		}

		this.setFoldRing(targetView)
	}

	/** Commits an armed fold if one is showing; @return whether it folded. */
	fun dropPinnedFold(): Boolean {
		this.foldHandler.removeCallbacksAndMessages(null)
		val target = this.foldArmedTarget ?: return false
		this.setFoldRing(null)

		return this.foldDraggedOnto(target)
	}

	/** Cancels any fold preview (drag ended / no drop on the bar). */
	fun cancelPinnedFold() {
		this.foldHandler.removeCallbacksAndMessages(null)
		this.setFoldRing(null)
	}

	/** Sets (or clears) the "release to fold here" ring on a pinned icon/folder. */
	private fun setFoldRing(view: View?) {
		if (this.foldArmedTarget === view) {
			return
		}
		this.foldArmedTarget?.foreground = null
		this.foldArmedTarget = view
		view?.foreground = this.activity.getDrawable(R.drawable.launcher_folder_drop_indicator)
	}

	/**
	 * Legacy hover used by the per-icon [be.robinj.distrohopper.desktop.launcher.AppLauncherDragListener]
	 * (and its tests); production drives reorder/fold spatially via
	 * [previewPinnedInsert] / [previewPinnedFold] from the container listener.
	 */
	fun hoverPinnedItem(targetView: View) {
		val dragged = this.draggedPinnedApp ?: return
		if (targetView === dragged) {
			return
		}

		this.draggedPinnedItemOver(targetView)
		this.foldHandler.removeCallbacksAndMessages(null)
		if (this.canFoldOnto(targetView)) {
			this.foldHandler.postDelayed({ this.setFoldRing(targetView) }, FOLD_DWELL_MS)
		}
	}

	/**
	 * Commits the order previewed by the placeholder's position. The bar's order
	 * is the pinned order, so this flattens the bar's items (folder members
	 * included, in membership order) into a key sequence and reorders the pinned
	 * model to match — which keeps the floating launcher service and persistence
	 * in step too.
	 */
	fun droppedPinnedApp() {
		if (this.draggedPinnedAppDropped) {
			return // a dwell-fold already committed this drop //
		}
		val dragged = this.draggedPinnedApp ?: return
		if (this.llLauncherPinnedApps.indexOfChild(dragged) < 0)
			return
		this.draggedPinnedAppDropped = true

		val desktop = this.appManager.currentDesktop

		// An app extracted from a folder is already pinned: ungroup it (the source
		// folder dissolves if it drops to one app), keeping the remaining members,
		// then position it where the placeholder is. flattenBarKeys is built to
		// exclude the extracted key from the stale source folder view so it is not
		// double-counted (the placeholder supplies it at the drop slot) //
		if (this.draggedPinnedAppOldIndex == EXTRACTED_FOLDER_MEMBER) {
			val app = dragged.tag as App
			val keys = this.flattenBarKeysExtracting(dragged, app.profileScopedKey)
			this.extractFromFolderId?.let {
				this.appManager.launcherLayout.removeFromFolder(it, app.profileScopedKey)
			}
			this.appManager.reorderPinned(desktop, keys)
			this.appManager.savePinnedApps()
			this.refreshPinnedView()
			return
		}

		if (this.draggedPinnedAppOldIndex == NOT_YET_PINNED &&
			!this.appManager.pin(dragged.tag as App, false, false)) {
			this.draggedPinnedAppDropped = false // pinned meanwhile: let ended rebuild //
			return
		}

		this.appManager.reorderPinned(desktop, this.flattenBarKeys())
		this.appManager.savePinnedApps()
		this.refreshPinnedView()
	}

	/** The bar's current item order flattened to pinned-app keys (folders expanded). */
	private fun flattenBarKeys(): List<String> {
		val keys = ArrayList<String>()
		for (i in 0 until this.llLauncherPinnedApps.childCount) {
			when (val child = this.llLauncherPinnedApps.getChildAt(i)) {
				is LauncherFolderView -> child.apps.forEach { keys.add(it.profileScopedKey) }
				else -> (child.tag as? App)?.let { keys.add(it.profileScopedKey) }
			}
		}

		return keys
	}

	/**
	 * [flattenBarKeys] for committing a folder-member extraction: the source folder
	 * view is still showing [extractedKey] (the model is only mutated right after),
	 * so drop exactly that occurrence from the folder's expansion. The dragged
	 * [placeholder] still carries the key at the drop slot, so the app ends up at
	 * the dropped position with the folder's other members left in place.
	 */
	private fun flattenBarKeysExtracting(placeholder: View, extractedKey: String): List<String> {
		val keys = ArrayList<String>()
		for (i in 0 until this.llLauncherPinnedApps.childCount) {
			when (val child = this.llLauncherPinnedApps.getChildAt(i)) {
				placeholder -> (child.tag as? App)?.let { keys.add(it.profileScopedKey) }
				is LauncherFolderView -> child.apps.forEach {
					if (it.profileScopedKey != extractedKey) keys.add(it.profileScopedKey)
				}
				else -> (child.tag as? App)?.let { keys.add(it.profileScopedKey) }
			}
		}

		return keys
	}

	/**
	 * Folds the dragged app onto [targetView] (a dwell-armed drop): create a
	 * folder with the target app, or add to the target folder. Folders cannot be
	 * folded, and a not-yet-pinned dash app is pinned first.
	 */
	fun foldDraggedOnto(targetView: View): Boolean {
		val dragged = this.draggedPinnedApp ?: return false
		val draggedApp = dragged.tag as? App ?: return false // a folder can't go into a folder
		if (targetView == dragged)
			return false
		val desktop = this.appManager.currentDesktop
		val layout = this.appManager.launcherLayout

		if (this.draggedPinnedAppOldIndex == NOT_YET_PINNED) {
			if (!this.appManager.pin(draggedApp, false, false)) {
				return false
			}
			this.appManager.savePinnedApps()
		} else if (this.draggedPinnedAppOldIndex == EXTRACTED_FOLDER_MEMBER) {
			// Already pinned (a folder member): ungroup it from its source folder
			// first, so folding it onto another icon/folder re-groups it cleanly //
			this.extractFromFolderId?.let {
				layout.removeFromFolder(it, draggedApp.profileScopedKey)
			}
		}
		this.draggedPinnedAppDropped = true

		if (targetView is LauncherFolderView) {
			if (!layout.addToFolder(targetView.folderId, draggedApp)) {
				Toast.makeText(this.activity, R.string.folder_full, Toast.LENGTH_SHORT).show()
			}
		} else {
			val targetApp = targetView.tag as? App
			if (targetApp == null || targetApp == draggedApp) {
				return false
			}
			layout.createFolder(desktop, draggedApp, targetApp)
		}

		this.refreshPinnedView()
		return true
	}

	/** Begins dragging a pinned folder (reposition / drop on trash to delete). */
	fun startedDraggingFolder(folderId: String) {
		val view = this.llLauncherPinnedApps.findViewWithTag<View>(folderId) ?: return
		this.draggedPinnedApp = view
		this.draggedPinnedAppOldIndex = this.llLauncherPinnedApps.indexOfChild(view)
		this.draggedPinnedAppDropped = false
		view.visibility = View.INVISIBLE

		this.startedDraggingPinnedApp()
	}

	fun endedDraggingPinnedApp() {
		val dragged = this.draggedPinnedApp ?: return
		this.draggedPinnedApp = null
		this.draggedPinnedAppOldIndex = -1
		// A cancelled extraction left the model untouched (the app is still in its
		// folder); the not-dropped refresh below rebuilds the bar to match it //
		this.extractFromFolderId = null

		dragged.visibility = View.VISIBLE
		if (!this.draggedPinnedAppDropped)
			this.refreshPinnedView() // Cancelled, or unpinned via the trash: snap back to the model's order //
	}

	fun stoppedDraggingPinnedApp() = stoppedDragging(this.activity)

	private fun pinnedAppLauncher(app: App): AppLauncher {
		val appLauncher = AppLauncher(this.activity, app)
		appLauncher.setOnClickListener(AppLauncherClickListener(this.activity))
		appLauncher.setOnLongClickListener(AppLauncherLongClickListener(this.activity))
		// Reorder/fold is hit-tested at the container level by LauncherDragListener
		// (nested icons don't receive drag LOCATION events), so no per-icon listener.

		return appLauncher
	}

	companion object {
		/** draggedPinnedAppOldIndex value for a dash app not yet in the pinned model. */
		private const val NOT_YET_PINNED = -1

		/** draggedPinnedAppOldIndex value for an app extracted from a launcher folder:
		 *  already pinned, so a committing drop ungroups rather than pins it. */
		private const val EXTRACTED_FOLDER_MEMBER = -2

		/** How long the drag must pause over a pinned app/folder to arm a fold. */
		private const val FOLD_DWELL_MS = 550L

		/*
		 * The drag decorations only touch views, not the app model, and widget
		 * drags can start before app loading has finished — so these are usable
		 * with just the activity.
		 */

		/**
		 * Whether the current drag shows the "app info" target in the trash's
		 * place. Set (by the dash's long-click listener) when an app icon is
		 * picked up from the dash — the dash always shows every installed app,
		 * so the trash would have nothing to delete there; opening the system's
		 * App info screen is the useful drop instead. Dash *folders* keep the
		 * trash (dropping one there deletes the folder). Sticky for the drag's
		 * lifetime — mid-drag [startedDragging] refreshers (e.g. hovering the
		 * launcher bar) re-apply it — and cleared by [stoppedDragging].
		 */
		private var dragShowsAppInfo = false

		/** Marks the drag that is starting as a dash app-icon drag (see [dragShowsAppInfo]). */
		@JvmStatic
		fun dragStartedFromDashApp() {
			dragShowsAppInfo = true
		}

		@JvmStatic
		fun startedDragging(activity: HomeActivity) {
			val viewFinder = activity.viewFinder
			val llLauncher = viewFinder.get<LinearLayout>(R.id.llLauncher)
			val lalBfb = viewFinder.get<AppLauncher>(llLauncher, R.id.lalBfb)
			val lalPreferences = viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
			val lalTrash = viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)
			val lalAppInfo = viewFinder.get<AppLauncher>(llLauncher, R.id.lalAppInfo)

			// The BFB stays visible during the drag: it is the "re-open the dash"
			// target for the cross-surface drag (hover it to bring the dash back),
			// and the dash is no longer auto-closed when a drag starts (folders and
			// in-dash reordering need it to stay open; the launcher sits beside the
			// dash so dropping there still pins). Hovering the launcher/panel closes
			// the dash to reveal the desktop — see Bfb/Launcher drag listeners.
			lalBfb.visibility = View.VISIBLE
			lalPreferences.visibility = View.GONE
			lalTrash.visibility = if (dragShowsAppInfo) View.GONE else View.VISIBLE
			lalAppInfo.visibility = if (dragShowsAppInfo) View.VISIBLE else View.GONE

			viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherPinnedApps).alpha = 0.9F

			viewFinder.get<WidgetsPager>(R.id.vgWidgets).showGridOverlay()
		}

		@JvmStatic
		fun stoppedDragging(activity: HomeActivity) {
			dragShowsAppInfo = false

			val viewFinder = activity.viewFinder
			val llLauncher = viewFinder.get<LinearLayout>(R.id.llLauncher)
			val lalBfb = viewFinder.get<AppLauncher>(llLauncher, R.id.lalBfb)
			val lalPreferences = viewFinder.get<AppLauncher>(llLauncher, R.id.lalPreferences)
			val lalTrash = viewFinder.get<AppLauncher>(llLauncher, R.id.lalTrash)
			val lalAppInfo = viewFinder.get<AppLauncher>(llLauncher, R.id.lalAppInfo)

			val theme = DependencyContainer.of(activity).themeManager.current
			val lalPreferences_location = theme.lalPreferences_getLocation(
				activity.resources, Preferences.getSharedPreferences(activity))
			lalBfb.visibility = View.VISIBLE
			lalPreferences.visibility =
				if (lalPreferences_location == Location.NONE) View.GONE else View.VISIBLE
			lalTrash.visibility = View.GONE
			lalAppInfo.visibility = View.GONE

			viewFinder.get<LinearLayout>(llLauncher, R.id.llLauncherPinnedApps).alpha = 1.0F

			viewFinder.get<WidgetsPager>(R.id.vgWidgets).hideGridOverlay()
		}
	}
}
