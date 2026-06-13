package be.robinj.distrohopper.desktop.dash

import android.animation.LayoutTransition
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
			ArrayList(this.appManager.repository.appsForProfile(profile)))
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
		// don't exist when that configurer runs.
		holder.grid.layoutTransition = LayoutTransition().apply {
			setDuration(180L)
			setStartDelay(LayoutTransition.APPEARING, 0)
		}

		return holder
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

		holder.grid.adapter = this.gridAdapters[position]
		holder.grid.onItemClickListener = AppLauncherClickListener(this.activity)
		holder.grid.onItemLongClickListener = AppLauncherLongClickListener(this.activity)
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
	}

	/** Refreshes every page's apps from the repository, preserving page scroll. */
	fun refresh() {
		for ((i, profile) in this.profiles.withIndex()) {
			val adapter = this.gridAdapters[i]
			adapter.setNotifyOnChange(false)
			adapter.clear()
			adapter.addAll(this.appManager.repository.appsForProfile(profile))
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
