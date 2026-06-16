package be.robinj.distrohopper.desktop.dash

import android.animation.LayoutTransition
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.GridView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.Profiles
import be.robinj.distrohopper.R

/**
 * Backs the dash profile ViewPager2: one page per profile, each a GridView
 * identical to the standard dash grid but scoped to that profile's apps.
 * One GridAdapter is kept per profile so app changes refresh in place
 * (preserving each page's scroll) and column-width changes apply live to the
 * pages currently attached.
 */
class ProfilePagerAdapter(
	private val activity: HomeActivity,
	private val appManager: AppManager,
	private val profiles: List<UserHandle?>,
) : RecyclerView.Adapter<ProfilePagerAdapter.PageViewHolder>() {

	private val gridAdapters: List<GridAdapter> = this.profiles.map { profile ->
		GridAdapter(this.activity.applicationContext,
			ArrayList(this.appManager.dashLayout.dashItems(profile)))
	}

	class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val title: TextView = view.findViewById(R.id.tvDashHomeTitle)
		val grid: GridView = view.findViewById(R.id.gvDashHomeApps)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.widget_dash_profile_page, parent, false)
		val holder = PageViewHolder(view)

		// The icon appear/disappear transition that LayoutTransitionConfigurer
		// used to set on the standalone grid; set per page here since the pages
		// don't exist when that configurer runs. setAnimateParentHierarchy(false)
		// is required: ViewPager2 rejects a page whose child ViewGroup has a
		// LayoutTransition that animates the parent hierarchy (the default), and
		// crashes mid-scroll on the next layout pass (e.g. on rotation) with
		// "...interferes with the scrolling animation".
		holder.grid.layoutTransition = LayoutTransition().apply {
			setDuration(180L)
			setStartDelay(LayoutTransition.APPEARING, 0)
			setAnimateParentHierarchy(false)
		}

		this.bindTitleCollapse(holder)

		return holder
	}

	/**
	 * Makes the page title scroll off-screen together with the apps instead of
	 * staying pinned above them. The title overlays the grid's top padding (see
	 * widget_dash_profile_page) and we translate it up in step with the grid's
	 * scroll, so it slides out of view as the first row of apps does.
	 *
	 * The padding that reserves the title's space is set in onBindViewHolder
	 * *before* the grid's adapter (so the first fill lands the first row at
	 * paddingTop); it must not change after the grid is populated, because
	 * AbsListView does not re-anchor already-laid-out rows when paddingTop
	 * changes at runtime (that was the cause of the title resting pre-collapsed
	 * on swiped-to pages). Here we only attach the listeners that read the
	 * scroll position and move the title:
	 *  - a scroll listener for live scrolling, and
	 *  - a layout-change listener so the at-rest baseline is recomputed after
	 *    any (re)fill or rotation — the scroll listener never fires for a page
	 *    whose apps fit without scrolling. Both only set translationY, which is
	 *    a draw-time transform, so they don't trigger further layout.
	 */
	private fun bindTitleCollapse(holder: PageViewHolder) {
		holder.grid.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
			this.updateTitleOffset(holder)
		}

		holder.grid.setOnScrollListener(object : AbsListView.OnScrollListener {
			override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
			override fun onScroll(view: AbsListView, firstVisibleItem: Int,
				visibleItemCount: Int, totalItemCount: Int) {
				this@ProfilePagerAdapter.updateTitleOffset(holder)
			}
		})
	}

	private fun updateTitleOffset(holder: PageViewHolder) {
		// The reserved space (== title height) is the grid's top padding; the
		// title is fully off-screen once scrolled by that much.
		val collapse = holder.grid.paddingTop
		if (collapse == 0) {
			holder.title.translationY = 0F
			return
		}

		// With the padding applied before the fill, the first row rests at
		// paddingTop; as the grid scrolls up that gap shrinks. Once the first
		// row has scrolled past the top the title is fully gone.
		val first = holder.grid.getChildAt(0)
		val scrolled = if (holder.grid.firstVisiblePosition == 0 && first != null) {
			collapse - first.top
		} else {
			collapse
		}
		holder.title.translationY = -scrolled.coerceIn(0, collapse).toFloat()
	}

	override fun getItemCount(): Int = this.profiles.size

	override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
		// Each page's title swipes with it: the profile name, or "Applications"
		// when there is only the personal profile (so the dash looks unchanged).
		holder.title.text = if (this.profiles.size == 1) {
			this.activity.getString(R.string.dash_lens_apps_title)
		} else {
			Profiles.label(this.activity, this.profiles[position])
		}
		val theme = DependencyContainer.of(this.activity).themeManager.current
		val res = this.activity.resources
		holder.title.setTextColor(res.getColor(theme.dash_applauncher_text_colour))
		holder.title.setShadowLayer(5F, 2F, 2F,
			res.getColor(theme.dash_applauncher_text_shadow_colour))

		// Reserve the title's space as grid top padding *before* assigning the
		// adapter, so the very first fill of this page lands the first row at
		// paddingTop. Changing padding after the grid is populated does not move
		// already-laid-out rows, which left the title pre-collapsed on swiped-to
		// pages. The title is a single line of identical height for every
		// profile, so a one-off measure is stable across pages.
		val titleHeight = this.measureTitleHeight(holder.title)
		if (holder.grid.paddingTop != titleHeight) {
			holder.grid.setPadding(holder.grid.paddingLeft, titleHeight,
				holder.grid.paddingRight, holder.grid.paddingBottom)
		}

		holder.grid.adapter = this.gridAdapters[position]
		holder.grid.onItemClickListener = AppLauncherClickListener(this.activity)
		holder.grid.onItemLongClickListener = AppLauncherLongClickListener(this.activity)
		// Dash-internal dragging: reorder (custom order), folder create/add, and
		// folder-member extraction, scoped to this page's profile.
		holder.grid.setOnDragListener(
			DashGridDragListener(this.activity, this.appManager, this.profiles[position]))

		// Fresh bind starts at scroll 0; reset the baseline so a recycled holder
		// doesn't carry a previous (scrolled) page's translation. The layout
		// listener corrects it once this page's rows are laid out.
		holder.title.translationY = 0F
	}

	/**
	 * The laid-out height of the single-line title, measured without needing the
	 * view to be attached/laid out yet, so it can be reserved as grid padding
	 * before the first fill.
	 */
	private fun measureTitleHeight(title: TextView): Int {
		val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		title.measure(unspecified, unspecified)
		return title.measuredHeight
	}

	override fun onViewAttachedToWindow(holder: PageViewHolder) {
		super.onViewAttachedToWindow(holder)

		// Apply the column count as the page comes on screen, not just at bind:
		// a page can re-attach from the recycler cache without re-binding (e.g.
		// swiped to after a rotation), and onConfigurationChanged only reaches
		// the grids attached at that moment — so an off-screen page would keep
		// the old orientation's count and render with hugely over/undersized
		// icons until rebound.
		DashGridSizer.apply(holder.grid)

		// A page can re-attach from the recycler cache without re-binding (so the
		// onBind translationY reset never runs); re-derive the title offset from
		// this page's own scroll position so it never shows a previous page's.
		this.updateTitleOffset(holder)
	}

	/** Refreshes every page's items (apps + folders) from the layout, preserving page scroll. */
	fun refresh() {
		for ((i, profile) in this.profiles.withIndex()) {
			val adapter = this.gridAdapters[i]
			adapter.setNotifyOnChange(false)
			adapter.clear()
			adapter.addAll(this.appManager.dashLayout.dashItems(profile))
			adapter.notifyDataSetChanged()
		}
	}

	/** Re-applies the unified column count to every attached page (live pref/rotation change). */
	fun applyColumns(viewPager: ViewPager2) {
		this.forEachAttachedGrid(viewPager) { DashGridSizer.apply(it) }
	}

	fun invalidatePages(viewPager: ViewPager2) {
		this.forEachAttachedGrid(viewPager) { it.invalidateViews() }
	}

	private fun forEachAttachedGrid(viewPager: ViewPager2, action: (GridView) -> Unit) {
		// ViewPager2 hosts its pages in a RecyclerView at child index 0; the grid
		// is the gvDashHomeApps inside each page (see widget_dash_profile_page).
		val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return
		for (i in 0 until recycler.childCount) {
			recycler.getChildAt(i).findViewById<GridView>(R.id.gvDashHomeApps)?.let(action)
		}
	}

	companion object {
		/**
		 * The GridView of the pager's current page, or null if no page is laid
		 * out yet. Only the current page is attached while the pager is idle, so
		 * this is also what `findViewById(R.id.gvDashHomeApps)` resolves to.
		 */
		fun currentGrid(viewPager: ViewPager2): GridView? {
			val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return null
			for (i in 0 until recycler.childCount) {
				val child = recycler.getChildAt(i)
				if (recycler.getChildLayoutPosition(child) == viewPager.currentItem) {
					return child.findViewById(R.id.gvDashHomeApps)
				}
			}

			return null
		}
	}
}
