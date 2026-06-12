package be.robinj.distrohopper.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Inflates one fixed layout per [OnboardingPage]. State lives in
 * [OnboardingActivity], which supplies [bind]; pages whose state changed
 * (permission granted, role held) are refreshed via [rebind].
 */
class OnboardingPagerAdapter(
	private val bind: (OnboardingPage, View) -> Unit,
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageHolder>() {
	class PageHolder(view: View) : RecyclerView.ViewHolder(view)

	override fun getItemCount(): Int = OnboardingPage.entries.size

	override fun getItemViewType(position: Int): Int = position

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder =
		PageHolder(
			LayoutInflater.from(parent.context)
				.inflate(OnboardingPage.entries[viewType].layout, parent, false)
		)

	override fun onBindViewHolder(holder: PageHolder, position: Int) =
		this.bind(OnboardingPage.entries[position], holder.itemView)

	fun rebind(page: OnboardingPage) = this.notifyItemChanged(page.ordinal)
}
