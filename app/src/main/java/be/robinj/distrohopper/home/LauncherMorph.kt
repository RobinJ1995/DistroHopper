package be.robinj.distrohopper.home

/**
 * The pure maths of the launcher's per-desktop pin morph: given the pinned items
 * of the desktop being swiped *from* and the one being swiped *to*, and a
 * progress fraction (0 = fully on `from`, 1 = fully on `to`), it places every
 * item involved at a slot position (in icon-slot units along the launcher's main
 * axis) with an alpha and scale.
 *
 * Shared items slide from their `from` slot to their `to` slot; items unique to
 * a desktop hold their slot and fade + scale (out as `from`-only, in as
 * `to`-only). The view layer multiplies the slot position by the icon stride.
 * Generic over the item type so it serves both plain apps and the launcher's
 * folder-aware [be.robinj.distrohopper.desktop.launcher.LauncherItem]s (a folder
 * id is unique to one desktop, so folders simply fade in/out).
 */
object LauncherMorph {
	/** Scale an appearing/disappearing item shrinks to/from. */
	const val EXIT_SCALE = 0.6F

	class Slot<T>(val item: T, val position: Float, val alpha: Float, val scale: Float)

	/** The items to render during the morph: `from` in order, then any `to`-only items. */
	@JvmStatic
	fun <T> union(from: List<T>, to: List<T>): List<T> {
		val result = ArrayList(from)
		for (item in to) {
			if (! result.contains(item)) {
				result.add(item)
			}
		}

		return result
	}

	@JvmStatic
	fun <T> slots(from: List<T>, to: List<T>, fraction: Float): List<Slot<T>> {
		val f = fraction.coerceIn(0F, 1F)

		return this.union(from, to).map { item ->
			val fromIndex = from.indexOf(item)
			val toIndex = to.indexOf(item)

			when {
				fromIndex >= 0 && toIndex >= 0 -> // shared: slide slot -> slot //
					Slot(item, lerp(fromIndex.toFloat(), toIndex.toFloat(), f), 1F, 1F)
				fromIndex >= 0 -> // leaving: hold slot, fade + shrink out //
					Slot(item, fromIndex.toFloat(), 1F - f, lerp(1F, EXIT_SCALE, f))
				else -> // arriving: hold slot, fade + grow in //
					Slot(item, toIndex.toFloat(), f, lerp(EXIT_SCALE, 1F, f))
			}
		}
	}

	private fun lerp(from: Float, to: Float, fraction: Float): Float =
		from + (to - from) * fraction
}
