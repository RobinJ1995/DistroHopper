package be.robinj.distrohopper.dev

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/** Counts the activities the process is still holding, for the memory readout. */
object HeapProbe {
	/** Bounded: the launcher's process lives for days. */
	private const val CAPACITY = 64

	private val lock = Any()
	private val queue = ReferenceQueue<Activity>()
	private val destroyed = ArrayList<WeakReference<Activity>>()
	private var created = 0
	private var buried = 0

	/** Unconditional: gated on a preference, it would miss what preceded the read. */
	fun register(application: Application) {
		application.registerActivityLifecycleCallbacks(Callbacks)
	}

	fun liveActivities(): Int = synchronized(this.lock) { this.created - this.buried }

	/** An upper bound, not a leak count: these may be pending collection. */
	fun uncollectedActivities(): Int = synchronized(this.lock) {
		this.prune()
		this.destroyed.size
	}

	/** Draining the queue first is what makes the count fall on the next read. */
	private fun prune() {
		while (this.queue.poll() != null) {
			// get() below is what removes the entry //
		}

		this.destroyed.removeAll { it.get() == null }
	}

	private object Callbacks : Application.ActivityLifecycleCallbacks {
		override fun onActivityCreated(activity: Activity, state: Bundle?) {
			synchronized(HeapProbe.lock) { HeapProbe.created++ }
		}

		override fun onActivityDestroyed(activity: Activity) {
			synchronized(HeapProbe.lock) {
				HeapProbe.buried++
				HeapProbe.prune()

				while (HeapProbe.destroyed.size >= CAPACITY) {
					HeapProbe.destroyed.removeAt(0)
				}

				HeapProbe.destroyed.add(WeakReference(activity, HeapProbe.queue))
			}
		}

		override fun onActivityStarted(activity: Activity) {}
		override fun onActivityResumed(activity: Activity) {}
		override fun onActivityPaused(activity: Activity) {}
		override fun onActivityStopped(activity: Activity) {}
		override fun onActivitySaveInstanceState(activity: Activity, out: Bundle) {}
	}
}
