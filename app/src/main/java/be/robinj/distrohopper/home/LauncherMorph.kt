package be.robinj.distrohopper.home

import be.robinj.distrohopper.App

/**
 * The pure maths of the launcher's per-desktop pin morph: given the pinned apps
 * of the desktop being swiped *from* and the one being swiped *to*, and a
 * progress fraction (0 = fully on `from`, 1 = fully on `to`), it places every
 * app involved at a slot position (in icon-slot units along the launcher's main
 * axis) with an alpha and scale.
 *
 * Shared apps slide from their `from` slot to their `to` slot; apps unique to a
 * desktop hold their slot and fade + scale (out as `from`-only, in as
 * `to`-only). The view layer multiplies the slot position by the icon stride.
 */
object LauncherMorph {
	/** Scale an appearing/disappearing app shrinks to/from. */
	const val EXIT_SCALE = 0.6F

	class Slot(val app: App, val position: Float, val alpha: Float, val scale: Float)

	/** The apps to render during the morph: `from` in order, then any `to`-only apps. */
	@JvmStatic
	fun union(from: List<App>, to: List<App>): List<App> {
		val result = ArrayList(from)
		for (app in to) {
			if (! result.contains(app)) {
				result.add(app)
			}
		}

		return result
	}

	@JvmStatic
	fun slots(from: List<App>, to: List<App>, fraction: Float): List<Slot> {
		val f = fraction.coerceIn(0F, 1F)

		return this.union(from, to).map { app ->
			val fromIndex = from.indexOf(app)
			val toIndex = to.indexOf(app)

			when {
				fromIndex >= 0 && toIndex >= 0 -> // shared: slide slot -> slot //
					Slot(app, lerp(fromIndex.toFloat(), toIndex.toFloat(), f), 1F, 1F)
				fromIndex >= 0 -> // leaving: hold slot, fade + shrink out //
					Slot(app, fromIndex.toFloat(), 1F - f, lerp(1F, EXIT_SCALE, f))
				else -> // arriving: hold slot, fade + grow in //
					Slot(app, toIndex.toFloat(), f, lerp(EXIT_SCALE, 1F, f))
			}
		}
	}

	private fun lerp(from: Float, to: Float, fraction: Float): Float =
		from + (to - from) * fraction
}
