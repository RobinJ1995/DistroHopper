package be.robinj.distrohopper.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import be.robinj.distrohopper.home.LauncherBarBinder
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
	private val edgeTop: ViewGroup
	private val edgeRight: ViewGroup
	private val edgeBottom: ViewGroup
	private val edgeLeft: ViewGroup
	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

	// Touch state for moving/resizing //
	private var startRawX = 0F
	private var startRawY = 0F
	private var startLeft = 0
	private var startTop = 0
	private var startWidth = 0
	private var startHeight = 0
	private var dragging = false
	private var systemDragStarted = false

	// Where the finger grabbed the widget, relative to its top-left corner;
	// used by WidgetsContainer_DragListener to position the landing indicator //
	internal var dragGrabOffsetX = 0
	internal var dragGrabOffsetY = 0

	private val info: AppWidgetProviderInfo?
		get() = this.widget.appWidgetInfo

	private val allowUnsupportedResize: Boolean
		get() = Preferences.getSharedPreferences(this.context)
			.getBoolean(Preference.DEV_WIDGET_RESIZE_ANY.getName(), false)

	var editMode = false
		set(value) {
			if (value) {
				// Only one widget in edit mode at a time, across all desktops //
				(this.parent?.parent as? WidgetsPager)?.exitEditMode()
					?: (this.parent as? WidgetsContainer)?.exitEditMode()
			}

			field = value

			val info = this.info
			val allowUnsupportedResize = this.allowUnsupportedResize
			val canResizeH = allowUnsupportedResize || info == null ||
				info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
			val canResizeV = allowUnsupportedResize || info == null ||
				info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0

			this.container.alpha = if (value) 0.8F else 1.0F
			this.overlay.visibility = if (value) VISIBLE else GONE
			this.edgeLeft.visibility = if (value && canResizeH) VISIBLE else GONE
			this.edgeRight.visibility = if (value && canResizeH) VISIBLE else GONE
			this.edgeTop.visibility = if (value && canResizeV) VISIBLE else GONE
			this.edgeBottom.visibility = if (value && canResizeV) VISIBLE else GONE
		}

	val appWidgetId: Int
		get() = this.widget.appWidgetId

	init {
		widget.setWidgetContainer(this)

		LayoutInflater.from(context).inflate(R.layout.widget_container, this, true)

		this.container = this.findViewById(R.id.widgetContainer)
		this.container.addView(widget)
		this.overlay = this.findViewById(R.id.widgetOverlayCenter)
		this.edgeTop = this.findViewById(R.id.llEdgeTop)
		this.edgeRight = this.findViewById(R.id.llEdgeRight)
		this.edgeBottom = this.findViewById(R.id.llEdgeBottom)
		this.edgeLeft = this.findViewById(R.id.llEdgeLeft)

		listOf(this.edgeTop, this.edgeRight, this.edgeBottom, this.edgeLeft)
			.forEach { it.setOnTouchListener(this) }
		this.overlay.setOnTouchListener(this)
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
				this.systemDragStarted = false

				return true
			}
			MotionEvent.ACTION_MOVE -> {
				// Stray moves can arrive between startDragAndDrop() and the framework
				// taking over the touch stream; they must not restart the drag //
				if (this.systemDragStarted) {
					return true
				}

				val dx = (e.rawX - this.startRawX).toInt()
				val dy = (e.rawY - this.startRawY).toInt()

				if (!this.dragging) {
					if (abs(dx) < this.touchSlop && abs(dy) < this.touchSlop) {
						return true
					}

					if (view.id == R.id.widgetOverlayCenter) {
						// The system drag shadow moves freely across the screen and can be
						// dropped on the launcher's trash icon //
						this.systemDragStarted = true
						this.startMoveDrag(parent, e)

						return true
					}

					this.dragging = true
					lp.previewLeftPx = this.startLeft
					lp.previewTopPx = this.startTop
					lp.previewWidthPx = this.startWidth
					lp.previewHeightPx = this.startHeight
				}

				val cellW = parent.cellWidth
				val cellH = parent.cellHeight

				// The drag itself is limited to whole cells within the active size
				// policy (provider limits normally, grid-only limits for the developer
				// override), so the preview can never exceed what a release would commit //
				val minColSpan = WidgetGrid.clampSpan(
					1, this.minResizeWidthPx(), this.maxResizeWidthPx(), cellW, WidgetGrid.COLS)
				val maxColSpan = WidgetGrid.clampSpan(
					WidgetGrid.COLS, this.minResizeWidthPx(), this.maxResizeWidthPx(), cellW, WidgetGrid.COLS)
				val minRowSpan = WidgetGrid.clampSpan(
					1, this.minResizeHeightPx(), this.maxResizeHeightPx(), cellH, WidgetGrid.ROWS)
				val maxRowSpan = WidgetGrid.clampSpan(
					WidgetGrid.ROWS, this.minResizeHeightPx(), this.maxResizeHeightPx(), cellH, WidgetGrid.ROWS)

				val gridLeft = parent.paddingLeft
				val gridTop = parent.paddingTop
				val gridRight = parent.width - parent.paddingRight
				val gridBottom = parent.height - parent.paddingBottom

				when (view.id) {
					R.id.llEdgeRight -> {
						val maxW = min(maxColSpan * cellW, gridRight - this.startLeft)
						val minW = min(minColSpan * cellW, maxW)
						lp.previewWidthPx = (this.startWidth + dx).coerceIn(minW, maxW)

						val snapX = gridLeft + WidgetGrid.snapToCell(
							lp.previewLeftPx + lp.previewWidthPx - gridLeft, cellW, WidgetGrid.COLS) * cellW
						parent.showSnapLine(true, snapX.toFloat(),
							lp.previewTopPx.toFloat(), (lp.previewTopPx + lp.previewHeightPx).toFloat())
					}
					R.id.llEdgeBottom -> {
						val maxH = min(maxRowSpan * cellH, gridBottom - this.startTop)
						val minH = min(minRowSpan * cellH, maxH)
						lp.previewHeightPx = (this.startHeight + dy).coerceIn(minH, maxH)

						val snapY = gridTop + WidgetGrid.snapToCell(
							lp.previewTopPx + lp.previewHeightPx - gridTop, cellH, WidgetGrid.ROWS) * cellH
						parent.showSnapLine(false, snapY.toFloat(),
							lp.previewLeftPx.toFloat(), (lp.previewLeftPx + lp.previewWidthPx).toFloat())
					}
					R.id.llEdgeLeft -> {
						val right = this.startLeft + this.startWidth
						val maxW = min(maxColSpan * cellW, right - gridLeft)
						val minW = min(minColSpan * cellW, maxW)
						val width = (this.startWidth - dx).coerceIn(minW, maxW)
						lp.previewLeftPx = right - width
						lp.previewWidthPx = width

						val snapX = gridLeft + WidgetGrid.snapToCell(
							lp.previewLeftPx - gridLeft, cellW, WidgetGrid.COLS - 1) * cellW
						parent.showSnapLine(true, snapX.toFloat(),
							lp.previewTopPx.toFloat(), (lp.previewTopPx + lp.previewHeightPx).toFloat())
					}
					R.id.llEdgeTop -> {
						val bottom = this.startTop + this.startHeight
						val maxH = min(maxRowSpan * cellH, bottom - gridTop)
						val minH = min(minRowSpan * cellH, maxH)
						val height = (this.startHeight - dy).coerceIn(minH, maxH)
						lp.previewTopPx = bottom - height
						lp.previewHeightPx = height

						val snapY = gridTop + WidgetGrid.snapToCell(
							lp.previewTopPx - gridTop, cellH, WidgetGrid.ROWS - 1) * cellH
						parent.showSnapLine(false, snapY.toFloat(),
							lp.previewLeftPx.toFloat(), (lp.previewLeftPx + lp.previewWidthPx).toFloat())
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
					parent.hideSnapLine()
					parent.requestLayout()
				}

				return true
			}
		}

		return false
	}

	private fun minResizeWidthPx(): Int = if (this.allowUnsupportedResize) 0 else this.info?.minResizeWidth ?: 0

	private fun minResizeHeightPx(): Int = if (this.allowUnsupportedResize) 0 else this.info?.minResizeHeight ?: 0

	/** The provider's maximum resize width in px, or 0 when unspecified. */
	private fun maxResizeWidthPx(): Int = if (this.allowUnsupportedResize) 0 else this.info?.maxResizeWidth ?: 0

	/** The provider's maximum resize height in px, or 0 when unspecified. */
	private fun maxResizeHeightPx(): Int = if (this.allowUnsupportedResize) 0 else this.info?.maxResizeHeight ?: 0

	private fun startMoveDrag(parent: WidgetsContainer, e: MotionEvent) {
		val location = IntArray(2)
		parent.getLocationOnScreen(location)
		this.dragGrabOffsetX = (e.rawX - location[0]).toInt() - this.left
		this.dragGrabOffsetY = (e.rawY - location[1]).toInt() - this.top

		val clip = ClipData.newPlainText("widget", this.appWidgetId.toString())
		this.startDragAndDrop(clip, DragShadowBuilder(this), this, 0)

		// Not via appManager: widgets are draggable before app loading finishes //
		(this.context as? HomeActivity)?.let { LauncherBarBinder.startedDragging(it) }
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

		val colSpan = WidgetGrid.clampSpan(
			max(1, colEnd - col), this.minResizeWidthPx(), this.maxResizeWidthPx(),
			cellWidth, WidgetGrid.COLS)
		val rowSpan = WidgetGrid.clampSpan(
			max(1, rowEnd - row), this.minResizeHeightPx(), this.maxResizeHeightPx(),
			cellHeight, WidgetGrid.ROWS)

		val candidate = WidgetLayout(this.appWidgetId, col, row, colSpan, rowSpan)

		if (!WidgetGrid.fits(parent.collectLayouts(this), candidate)) {
			return // Revert to the previous position //
		}

		lp.col = candidate.col
		lp.row = candidate.row
		lp.colSpan = candidate.colSpan
		lp.rowSpan = candidate.rowSpan

		this.widgetHost.persist()
	}

	/**
	 * Move the widget to the given cell, keeping its size; reverts if it does not fit.
	 */
	internal fun commitMove(col: Int, row: Int) {
		val parent = this.parent as? WidgetsContainer ?: return
		val lp = this.layoutParams as WidgetsContainer.LayoutParams

		val candidate = WidgetLayout(this.appWidgetId, col, row, lp.colSpan, lp.rowSpan)

		if (!WidgetGrid.fits(parent.collectLayouts(this), candidate)) {
			return // Revert to the previous position //
		}

		lp.col = candidate.col
		lp.row = candidate.row

		this.widgetHost.persist()
		parent.requestLayout()
	}
}
