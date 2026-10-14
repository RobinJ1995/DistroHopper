package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.content.res.Configuration
import android.database.DataSetObserver
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs

/**
 * The dash search results: one view per [LensSearchResultCollection], stacked
 * inside a ScrollView, each built by [CollectionGridAdapter].
 *
 * A section keeps its own view for as long as it is listed, so a streamed
 * result only rebinds the views already there. (A ListView re-measured every
 * section on each change and recycled one section's view for another, making
 * each grid re-inflate its cells for every streamed result.)
 */
class LensResultsList @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
	private var adapter: CollectionGridAdapter? = null
	private val sections = IdentityHashMap<LensSearchResultCollection, View>()
	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
	private var downY = 0F

	private val observer = object : DataSetObserver() {
		override fun onChanged() = this@LensResultsList.sync()
		override fun onInvalidated() = this@LensResultsList.sync()
	}

	init {
		this.orientation = VERTICAL
	}

	/** Shows [adapter]'s sections, replacing the previous search's at once. */
	fun setAdapter(adapter: CollectionGridAdapter?) {
		this.adapter?.unregisterDataSetObserver(this.observer)
		this.adapter = adapter
		adapter?.registerDataSetObserver(this.observer)

		// Old results vanish instantly rather than fading out under the new ones //
		val transition = this.layoutTransition
		this.layoutTransition = null
		this.removeAllViews()
		this.layoutTransition = transition
		this.sections.clear()
		(this.parent as? ScrollView)?.scrollTo(0, 0)

		this.sync()
	}

	/** Binds every listed section into its own view, adding new ones in order. */
	private fun sync() {
		val adapter = this.adapter ?: return
		val listed = Collections.newSetFromMap(IdentityHashMap<LensSearchResultCollection, Boolean>())

		for (i in 0 until adapter.count) {
			val section = adapter.getItem(i) ?: continue
			listed.add(section)

			val view = adapter.getView(i, this.sections[section], this)
			this.sections[section] = view
			if (this.getChildAt(i) !== view) {
				(view.parent as? ViewGroup)?.removeView(view)
				this.addView(view, i)
			}
		}

		while (this.childCount > adapter.count)
			this.removeViewAt(this.childCount - 1)
		this.sections.keys.retainAll(listed)
	}

	// Each section's grid shows one row and must never scroll through the rest
	// itself. Claim vertical drags, as the ListView this replaced did, so the
	// grid is cancelled instead. The ScrollView above still intercepts first
	// whenever it has something to scroll //
	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		when (ev.actionMasked) {
			MotionEvent.ACTION_DOWN -> this.downY = ev.y
			MotionEvent.ACTION_MOVE -> return abs(ev.y - this.downY) > this.touchSlop
		}

		return false
	}

	@android.annotation.SuppressLint("ClickableViewAccessibility") // consumes drags, no click
	override fun onTouchEvent(ev: MotionEvent): Boolean = ev.actionMasked != MotionEvent.ACTION_DOWN

	// Rotation doesn't recreate the activity; rebinding re-applies the grids'
	// column count for the new orientation (see DashGridSizer) //
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		this.sync()
	}
}
