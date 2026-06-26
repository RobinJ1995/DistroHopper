package be.robinj.distrohopper.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Inflates one fixed layout per page of [pages] (the [OnboardingPage]s the
 * wizard shows on this device). State lives in [OnboardingActivity], which
 * supplies [bind]; pages whose state changed (permission granted, role held)
 * are refreshed via [rebind].
 */
class OnboardingPagerAdapter(
	private val pages: List<OnboardingPage>,
	private val bind: (OnboardingPage, View) -> Unit,
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageHolder>() {
	class PageHolder(view: View) : RecyclerView.ViewHolder(view)

	override fun getItemCount(): Int = this.pages.size

	override fun getItemViewType(position: Int): Int = position

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
		PageHolder(
			LayoutInflater.from(parent.context)
				.inflate(this.pages[viewType].layout, parent, false)
		)

	override fun onBindViewHolder(holder: PageHolder, position: Int) =
		this.bind(this.pages[position], holder.itemView)

	/** No-op for pages this device doesn't show. */
	fun rebind(page: OnboardingPage) {
		val position = this.pages.indexOf(page)

		if (position >= 0) {
			this.notifyItemChanged(position)
		}
	}
}
