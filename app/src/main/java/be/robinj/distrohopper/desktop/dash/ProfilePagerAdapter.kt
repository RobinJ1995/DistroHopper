package be.robinj.distrohopper.desktop.dash

import android.animation.LayoutTransition
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.HomeActivity
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
	private val displayDensity: Float,
	private var dashIconWidth: Int,
) : RecyclerView.Adapter<ProfilePagerAdapter.PageViewHolder>() {

	private val gridAdapters: List<GridAdapter> = this.profiles.map { profile ->
		GridAdapter(this.activity.applicationContext,
			ArrayList(this.appManager.repository.appsForProfile(profile)),
			this.displayDensity, this.dashIconWidth)
	}

	class PageViewHolder(val grid: GridView) : RecyclerView.ViewHolder(grid)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
		val grid = LayoutInflater.from(parent.context)
			.inflate(R.layout.widget_dash_profile_page, parent, false) as GridView

		// The icon appear/disappear transition that LayoutTransitionConfigurer
		// used to set on the standalone grid; set per page here since the pages
		// don't exist when that configurer runs.
		grid.layoutTransition = LayoutTransition().apply {
			setDuration(180L)
			setStartDelay(LayoutTransition.APPEARING, 0)
		}

		return PageViewHolder(grid)
	}

	override fun getItemCount(): Int = this.profiles.size

	override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
		holder.grid.adapter = this.gridAdapters[position]
		holder.grid.setColumnWidth(columnWidthPx(this.displayDensity, this.dashIconWidth))
		holder.grid.onItemClickListener = AppLauncherClickListener(this.activity)
		holder.grid.onItemLongClickListener = AppLauncherLongClickListener(this.activity)
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

	fun applyIconWidth(viewPager: ViewPager2, dashIconWidth: Int) {
		this.dashIconWidth = dashIconWidth
		val width = columnWidthPx(this.displayDensity, dashIconWidth)
		this.forEachAttachedGrid(viewPager) { it.setColumnWidth(width) }
	}

	fun invalidatePages(viewPager: ViewPager2) {
		this.forEachAttachedGrid(viewPager) { it.invalidateViews() }
	}

	private fun forEachAttachedGrid(viewPager: ViewPager2, action: (GridView) -> Unit) {
		// ViewPager2 hosts its pages in a RecyclerView at child index 0; each
		// page's itemView is the GridView itself (see onCreateViewHolder).
		val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return
		for (i in 0 until recycler.childCount) {
			(recycler.getChildAt(i) as? GridView)?.let(action)
		}
	}

	companion object {
		fun columnWidthPx(density: Float, dashIconWidth: Int): Int =
			Math.round((80 + dashIconWidth) * density) // 80 is the minimum //

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
					return child as? GridView
				}
			}

			return null
		}
	}
}
