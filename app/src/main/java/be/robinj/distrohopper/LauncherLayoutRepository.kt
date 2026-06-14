package be.robinj.distrohopper

import android.content.Context
import be.robinj.distrohopper.desktop.launcher.LauncherItem
import be.robinj.distrohopper.folder.Folder
import be.robinj.distrohopper.folder.FolderGrid
import be.robinj.distrohopper.folder.FolderMember
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The launcher's folders and manual item order, layered over the pinned apps
 * held by [AppRepository] — the launcher counterpart to [DashLayoutRepository],
 * partitioned per **desktop**. Produces the [LauncherItem] list the pinned bar
 * renders (loose pinned apps + folders, in the manual order), and owns the
 * folder mutations (each persists via [LauncherLayoutStorage] and bumps
 * [revision]).
 *
 * Pinned apps keep living in [AppRepository] / [PinnedAppsStorage] unchanged;
 * this layer only records which of them are grouped into folders and the order
 * they appear in, so a missing/unreadable layout degrades to every pinned app
 * rendering loose. Unlike the dash, **deleting a launcher folder unpins its
 * members** (the spec: the folder and its members leave the launcher).
 *
 * Not internally synchronised: mutations/reads happen on the UI thread.
 */
class LauncherLayoutRepository(
	private val context: Context,
	private val appRepository: AppRepository,
) {
	private class DesktopState {
		val folders = ArrayList<Folder>()
		val order = ArrayList<String>()
	}

	private val states = HashMap<Int, DesktopState>()

	private val _revision = MutableStateFlow(0L)
	val revision: StateFlow<Long> = this._revision.asStateFlow()

	// --- Loading / saving --------------------------------------------------

	fun load() {
		this.states.clear()
		for ((desktop, layout) in LauncherLayoutStorage.read(this.prefs())) {
			val state = DesktopState()
			state.folders.addAll(layout.folders)
			state.order.addAll(layout.order)
			this.states[desktop] = state
		}

		this.reconcile()
		this.bump()
	}

	/** Drops folder members / order entries for apps no longer pinned on that desktop. */
	fun reconcile() {
		var changed = false

		for ((desktop, state) in this.states) {
			val pinnedKeys = this.appRepository.pinnedOn(desktop).mapTo(HashSet()) { it.profileScopedKey }

			var i = 0
			while (i < state.folders.size) {
				val folder = state.folders[i]
				val kept = folder.members.filter {
					it !is FolderMember.AppMember || pinnedKeys.contains(it.key)
				}
				val keptApps = kept.filterIsInstance<FolderMember.AppMember>()
				if (keptApps.size <= 1) {
					state.folders.removeAt(i)
					this.spliceFolderOut(state, desktop, folder.id, keptApps.map { it.key })
					changed = true
					continue
				} else if (kept.size != folder.members.size) {
					state.folders[i] = folder.copy(members = kept)
					changed = true
				}
				i++
			}

			val folderIds = state.folders.mapTo(HashSet()) { it.id }
			val iterator = state.order.iterator()
			while (iterator.hasNext()) {
				val id = iterator.next()
				val valid = when {
					id.startsWith(APP_PREFIX) -> pinnedKeys.contains(id.removePrefix(APP_PREFIX))
					id.startsWith(FOLDER_PREFIX) -> folderIds.contains(id.removePrefix(FOLDER_PREFIX))
					else -> false
				}
				if (!valid) {
					iterator.remove()
					changed = true
				}
			}
		}

		if (changed) {
			this.save()
		}
	}

	private fun save() {
		LauncherLayoutStorage.write(this.prefs(), this.states.mapValues { (_, state) ->
			LauncherLayoutStorage.DesktopLayout(state.folders.toList(), state.order.toList())
		})
		this.bump()
	}

	private fun bump() {
		this._revision.value = this._revision.value + 1
	}

	private fun prefs() = Preferences.getSharedPreferences(this.context, Preferences.LAUNCHER_LAYOUT)

	// --- Reading -----------------------------------------------------------

	/** The launcher items for [desktop]'s pinned bar, in the manual order. */
	fun launcherItems(desktop: Int): List<LauncherItem> {
		val state = this.states[desktop]
		val pinnedApps = this.appRepository.pinnedOn(desktop)
		val byKey = pinnedApps.associateBy { it.profileScopedKey }

		val items = ArrayList<LauncherItem>()
		val folderedKeys = HashSet<String>()
		if (state != null) {
			for (folder in state.folders) {
				val apps = folder.appKeys.mapNotNull { byKey[it] }
				if (apps.size >= 2) {
					items.add(LauncherItem.LauncherFolder(folder, apps))
					apps.forEach { folderedKeys.add(it.profileScopedKey) }
				}
			}
		}

		for (app in pinnedApps) {
			if (app.profileScopedKey !in folderedKeys) {
				items.add(LauncherItem.LauncherApp(app))
			}
		}

		return this.ordered(items, state, pinnedApps)
	}

	private fun ordered(
		items: List<LauncherItem>,
		state: DesktopState?,
		pinnedApps: List<App>,
	): List<LauncherItem> {
		val index = HashMap<String, Int>()
		state?.order?.forEachIndexed { i, id -> index[id] = i }

		// Items absent from the manual order keep their underlying pinned order.
		val pinnedIndex = HashMap<String, Int>()
		pinnedApps.forEachIndexed { i, app -> pinnedIndex[app.profileScopedKey] = i }
		fun fallback(item: LauncherItem): Int = when (item) {
			is LauncherItem.LauncherApp -> pinnedIndex[item.app.profileScopedKey] ?: Int.MAX_VALUE
			is LauncherItem.LauncherFolder ->
				item.apps.minOfOrNull { pinnedIndex[it.profileScopedKey] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
		}

		return items.sortedWith(
			compareBy({ index[this.itemId(it)] ?: Int.MAX_VALUE }, { fallback(it) }))
	}

	// --- Mutations ---------------------------------------------------------

	/** Creates a folder of [a] then [b] (both pinned on [desktop]); returns its id. */
	fun createFolder(desktop: Int, a: App, b: App): String? {
		if (a == b) {
			return null
		}

		val state = this.states.getOrPut(desktop) { DesktopState() }
		this.removeKeyFromFolders(state, desktop, a.profileScopedKey)
		this.removeKeyFromFolders(state, desktop, b.profileScopedKey)

		val folder = Folder(Folder.newId(), listOf(
			FolderMember.AppMember(a.profileScopedKey),
			FolderMember.AppMember(b.profileScopedKey),
		))
		state.folders.add(folder)

		state.order.remove(this.appItemId(b.profileScopedKey))
		val aId = this.appItemId(a.profileScopedKey)
		val pos = state.order.indexOf(aId)
		if (pos >= 0) {
			state.order[pos] = this.folderItemId(folder.id)
		} else {
			state.order.add(this.folderItemId(folder.id))
		}

		this.save()
		return folder.id
	}

	/** Adds [app] to [folderId]; false (caller toasts) when the folder is full. */
	fun addToFolder(folderId: String, app: App): Boolean {
		val (desktop, _) = this.locate(folderId) ?: return false
		val state = this.states[desktop] ?: return false

		val current = state.folders.first { it.id == folderId }
		if (current.appKeys.contains(app.profileScopedKey)) {
			return true
		}
		if (current.appKeys.size >= MAX_FOLDER_APPS) {
			return false
		}

		this.removeKeyFromFolders(state, desktop, app.profileScopedKey)
		val index = state.folders.indexOfFirst { it.id == folderId }
		if (index < 0) {
			return false
		}
		val folder = state.folders[index]
		state.folders[index] = folder.copy(
			members = folder.members + FolderMember.AppMember(app.profileScopedKey))
		state.order.remove(this.appItemId(app.profileScopedKey))

		this.save()
		return true
	}

	/** Removes the app from the folder; it stays pinned, loose again. Dissolves at ≤1. */
	fun removeFromFolder(folderId: String, appKey: String) {
		val (desktop, index) = this.locate(folderId) ?: return
		val state = this.states[desktop] ?: return
		val folder = state.folders[index]
		if (!folder.appKeys.contains(appKey)) {
			return
		}

		val kept = folder.members.filterNot {
			it is FolderMember.AppMember && it.key == appKey
		}
		val keptApps = kept.filterIsInstance<FolderMember.AppMember>()
		if (keptApps.size <= 1) {
			state.folders.removeAt(index)
			this.spliceFolderOut(state, desktop, folderId, keptApps.map { it.key } + appKey)
		} else {
			state.folders[index] = folder.copy(members = kept)
			this.insertAfterFolder(state, folderId, appKey)
		}

		this.save()
	}

	/** Reorders a member within a folder. */
	fun moveAppInFolder(folderId: String, fromIndex: Int, toIndex: Int) {
		val (desktop, index) = this.locate(folderId) ?: return
		val state = this.states[desktop] ?: return
		val folder = state.folders[index]
		val members = folder.members.toMutableList()
		if (fromIndex !in members.indices || toIndex !in members.indices) {
			return
		}

		members.add(toIndex, members.removeAt(fromIndex))
		state.folders[index] = folder.copy(members = members)
		this.save()
	}

	/**
	 * Deletes the folder AND unpins its member apps from [desktop] (the launcher
	 * spec: the folder and its members leave the launcher).
	 */
	fun deleteFolder(folderId: String) {
		val (desktop, index) = this.locate(folderId) ?: return
		val state = this.states[desktop] ?: return
		val folder = state.folders.removeAt(index)
		this.spliceFolderOut(state, desktop, folderId, emptyList())

		val byKey = this.appRepository.installedAppsMap()
		for (key in folder.appKeys) {
			byKey[key]?.let { this.appRepository.unpin(it, desktop) }
		}
		this.appRepository.savePinnedApps()

		this.save()
	}

	/** Applies a manual reorder on [desktop]'s bar (capture order, move, store). */
	fun moveItem(desktop: Int, fromIndex: Int, toIndex: Int) {
		val items = this.launcherItems(desktop)
		if (fromIndex !in items.indices || toIndex !in items.indices) {
			return
		}

		val ids = items.mapTo(ArrayList()) { this.itemId(it) }
		ids.add(toIndex, ids.removeAt(fromIndex))

		val state = this.states.getOrPut(desktop) { DesktopState() }
		state.order.clear()
		state.order.addAll(ids)
		this.save()
	}

	// --- Helpers -----------------------------------------------------------

	private fun locate(folderId: String): Pair<Int, Int>? {
		for ((desktop, state) in this.states) {
			val index = state.folders.indexOfFirst { it.id == folderId }
			if (index >= 0) {
				return desktop to index
			}
		}

		return null
	}

	private fun removeKeyFromFolders(state: DesktopState, desktop: Int, key: String) {
		var i = 0
		while (i < state.folders.size) {
			val folder = state.folders[i]
			if (folder.appKeys.contains(key)) {
				val kept = folder.members.filterNot {
					it is FolderMember.AppMember && it.key == key
				}
				val keptApps = kept.filterIsInstance<FolderMember.AppMember>()
				if (keptApps.size <= 1) {
					state.folders.removeAt(i)
					this.spliceFolderOut(state, desktop, folder.id, keptApps.map { it.key })
					continue
				} else {
					state.folders[i] = folder.copy(members = kept)
				}
			}
			i++
		}
	}

	/**
	 * Replaces the folder's order slot with its now-loose [appKeys], reinserted in
	 * their underlying pinned order (so a dissolved folder's apps return to their
	 * natural launcher positions rather than the membership order).
	 */
	private fun spliceFolderOut(state: DesktopState, desktop: Int, folderId: String, appKeys: List<String>) {
		val pinnedIndex = HashMap<String, Int>()
		this.appRepository.pinnedOn(desktop).forEachIndexed { i, app ->
			pinnedIndex[app.profileScopedKey] = i
		}
		val appIds = appKeys.sortedBy { pinnedIndex[it] ?: Int.MAX_VALUE }.map { this.appItemId(it) }
		appIds.forEach { state.order.remove(it) }

		val pos = state.order.indexOf(this.folderItemId(folderId))
		if (pos >= 0) {
			state.order.removeAt(pos)
			state.order.addAll(pos, appIds)
		} else {
			state.order.addAll(appIds)
		}
	}

	private fun insertAfterFolder(state: DesktopState, folderId: String, appKey: String) {
		val appId = this.appItemId(appKey)
		state.order.remove(appId)
		val pos = state.order.indexOf(this.folderItemId(folderId))
		if (pos >= 0) {
			state.order.add(pos + 1, appId)
		} else {
			state.order.add(appId)
		}
	}

	private fun appItemId(key: String) = APP_PREFIX + key
	private fun folderItemId(id: String) = FOLDER_PREFIX + id

	private fun itemId(item: LauncherItem): String = when (item) {
		is LauncherItem.LauncherApp -> this.appItemId(item.app.profileScopedKey)
		is LauncherItem.LauncherFolder -> this.folderItemId(item.folder.id)
	}

	companion object {
		/** Max apps per launcher folder (a 3x3 contents grid). */
		const val MAX_FOLDER_APPS = FolderGrid.MAX_CELLS

		private const val APP_PREFIX = "app:"
		private const val FOLDER_PREFIX = "folder:"
	}
}
