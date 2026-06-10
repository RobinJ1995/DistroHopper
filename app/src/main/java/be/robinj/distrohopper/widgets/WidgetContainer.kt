package be.robinj.distrohopper.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import be.robinj.distrohopper.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by robin on 18/01/15.
 */
class WidgetContainer internal constructor(
	context: Context,
	private val widgetHost: WidgetHost,
	private val widget: WidgetHostView,
) : FrameLayout(context), View.OnTouchListener {
	private val container: FrameLayout
	private val overlay: ViewGroup
	private val edges: List<ViewGroup>
	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

	// Touch state for moving/resizing //
	private var startRawX = 0F
	private var startRawY = 0F
	private var startLeft = 0
	private var startTop = 0
	private var startWidth = 0
	private var startHeight = 0
	private var dragging = false

	var editMode = false
		set(value) {
			if (value) {
				// Only one widget in edit mode at a time //
				(this.parent as? WidgetsContainer)?.exitEditMode()
			}

			field = value

			this.container.alpha = if (value) 0.8F else 1.0F
			this.overlay.visibility = if (value) VISIBLE else GONE
			this.edges.forEach { it.visibility = if (value) VISIBLE else GONE }
		}

	val appWidgetId: Int
		get() = this.widget.appWidgetId

	init {
		widget.setWidgetContainer(this)

		LayoutInflater.from(context).inflate(R.layout.widget_container, this, true)

		this.container = this.findViewById(R.id.widgetContainer)
		this.container.addView(widget)
		this.overlay = this.findViewById(R.id.widgetOverlayCenter)
		this.edges = listOf(
			this.findViewById(R.id.llEdgeTop),
			this.findViewById(R.id.llEdgeRight),
			this.findViewById(R.id.llEdgeBottom),
			this.findViewById(R.id.llEdgeLeft),
		)

		this.edges.forEach { it.setOnTouchListener(this) }
		this.overlay.setOnTouchListener(this)
		this.findViewById<ImageButton>(R.id.ibRemove)
			.setOnClickListener(WidgetContainerRemove_ClickListener(this))
	}

	fun removeWidget() {
		this.widgetHost.removeWidget(this)
	}

	override fun onTouch(view: View, e: MotionEvent): Boolean {
		val parent = this.parent as? WidgetsContainer ?: return false
		val lp = this.layoutParams as WidgetsContainer.LayoutParams

		when (e.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				this.startRawX = e.rawX
				this.startRawY = e.rawY
				this.startLeft = this.left
				this.startTop = this.top
				this.startWidth = this.width
				this.startHeight = this.height
				this.dragging = false

				return true
			}
			MotionEvent.ACTION_MOVE -> {
				val dx = (e.rawX - this.startRawX).toInt()
				val dy = (e.rawY - this.startRawY).toInt()

				if (!this.dragging) {
					if (abs(dx) < this.touchSlop && abs(dy) < this.touchSlop) {
						return true
					}

					this.dragging = true
					lp.previewLeftPx = this.startLeft
					lp.previewTopPx = this.startTop
					lp.previewWidthPx = this.startWidth
					lp.previewHeightPx = this.startHeight
				}

				val minSize = min(parent.cellWidth, parent.cellHeight) / 2

				when (view.id) {
					R.id.widgetOverlayCenter -> {
						lp.previewLeftPx = this.startLeft + dx
						lp.previewTopPx = this.startTop + dy
					}
					R.id.llEdgeRight -> {
						lp.previewWidthPx = max(minSize, this.startWidth + dx)
					}
					R.id.llEdgeBottom -> {
						lp.previewHeightPx = max(minSize, this.startHeight + dy)
					}
					R.id.llEdgeLeft -> {
						val clampedDx = min(dx, this.startWidth - minSize)
						lp.previewLeftPx = this.startLeft + clampedDx
						lp.previewWidthPx = this.startWidth - clampedDx
					}
					R.id.llEdgeTop -> {
						val clampedDy = min(dy, this.startHeight - minSize)
						lp.previewTopPx = this.startTop + clampedDy
						lp.previewHeightPx = this.startHeight - clampedDy
					}
				}

				parent.requestLayout()

				return true
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				if (this.dragging) {
					this.dragging = false

					if (e.actionMasked == MotionEvent.ACTION_UP) {
						this.commitPreview(parent, lp)
					}

					lp.clearPreview()
					parent.requestLayout()
				}

				return true
			}
		}

		return false
	}

	/**
	 * Snap the pixel preview position back to grid cells; keep it only if it fits.
	 */
	private fun commitPreview(parent: WidgetsContainer, lp: WidgetsContainer.LayoutParams) {
		val cellWidth = parent.cellWidth
		val cellHeight = parent.cellHeight

		if (cellWidth <= 0 || cellHeight <= 0) {
			return
		}

		val col = WidgetGrid.snapToCell(lp.previewLeftPx - parent.paddingLeft, cellWidth, WidgetGrid.COLS - 1)
		val row = WidgetGrid.snapToCell(lp.previewTopPx - parent.paddingTop, cellHeight, WidgetGrid.ROWS - 1)
		val colEnd = WidgetGrid.snapToCell(lp.previewLeftPx + lp.previewWidthPx - parent.paddingLeft, cellWidth, WidgetGrid.COLS)
		val rowEnd = WidgetGrid.snapToCell(lp.previewTopPx + lp.previewHeightPx - parent.paddingTop, cellHeight, WidgetGrid.ROWS)

		val candidate = WidgetLayout(
			this.appWidgetId, col, row, max(1, colEnd - col), max(1, rowEnd - row))

		if (!WidgetGrid.fits(parent.collectLayouts(this), candidate)) {
			return // Revert to the previous position //
		}

		lp.col = candidate.col
		lp.row = candidate.row
		lp.colSpan = candidate.colSpan
		lp.rowSpan = candidate.rowSpan

		this.widgetHost.persist()
	}
}
