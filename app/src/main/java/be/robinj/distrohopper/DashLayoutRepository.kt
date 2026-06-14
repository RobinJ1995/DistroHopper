package be.robinj.distrohopper

import android.content.Context
import android.os.UserHandle
import be.robinj.distrohopper.desktop.dash.DashItem
import be.robinj.distrohopper.folder.Folder
import be.robinj.distrohopper.folder.FolderGrid
import be.robinj.distrohopper.folder.FolderMember
import be.robinj.distrohopper.preferences.AppSortOrder
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The dash's folders and manual ("custom") order, layered over the installed
 * apps held by [AppRepository]. Produces the heterogeneous [DashItem] list the
 * dash grid renders, and owns the folder/order mutations (each persists via
 * [DashLayoutStorage] and bumps [revision] so the dash refreshes).
 *
 * Layout is partitioned per profile (the dash has one page per profile); a
 * folder belongs to the profile of its member apps. Uniqueness within the dash
 * is structural: a folder removes its members from the loose grid, and a key
 * lives in at most one folder, so an app can never appear twice.
 *
 * Not internally synchronised: all mutations and reads happen on the UI thread,
 * like the dash grid adapter that consumes them.
 */
class DashLayoutRepository(
	private val context: Context,
	private val appRepository: AppRepository,
) {
	private class ProfileState {
		val folders = ArrayList<Folder>()
		/** Manual order of item ids; items absent sort after, alphabetically. */
		val order = ArrayList<String>()
	}

	private val states = HashMap<String, ProfileState>()

	private val _revision = MutableStateFlow(0L)
	/** Bumped on every change so the dash views can refresh. */
	val revision: StateFlow<Long> = this._revision.asStateFlow()

	// --- Loading / saving --------------------------------------------------

	fun load() {
		this.states.clear()
		for ((profileKey, layout) in DashLayoutStorage.read(this.prefs())) {
			val state = ProfileState()
			state.folders.addAll(layout.folders)
			state.order.addAll(layout.order)
			this.states[profileKey] = state
		}

		this.reconcile()
		this.bump()
	}

	/**
	 * Drops folder members / order entries for apps that are no longer installed,
	 * dissolving any folder left with a single app. Call when the installed list
	 * changes (install/uninstall).
	 */
	fun reconcile() {
		val installed = this.appRepository.installedAppsMap().keys
		var changed = false

		for (state in this.states.values) {
			var i = 0
			while (i < state.folders.size) {
				val folder = state.folders[i]
				val kept = folder.members.filter {
					it !is FolderMember.AppMember || installed.contains(it.key)
				}
				val keptApps = kept.filterIsInstance<FolderMember.AppMember>()
				if (keptApps.size <= 1) {
					state.folders.removeAt(i)
					this.spliceFolderOut(state, folder.id, keptApps.map { it.key })
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
					id.startsWith(APP_PREFIX) -> installed.contains(id.removePrefix(APP_PREFIX))
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
		DashLayoutStorage.write(this.prefs(), this.states.mapValues { (_, state) ->
			DashLayoutStorage.ProfileLayout(state.folders.toList(), state.order.toList())
		})
		this.bump()
	}

	private fun bump() {
		this._revision.value = this._revision.value + 1
	}

	private fun prefs() = Preferences.getSharedPreferences(this.context, Preferences.DASH_LAYOUT)

	// --- Reading -----------------------------------------------------------

	/** The dash items for [profile]'s page, ordered by the active sort order. */
	fun dashItems(profile: UserHandle?): List<DashItem> {
		val state = this.states[this.profileKey(profile)]
		val profileApps = this.appRepository.appsForProfile(profile)
		val byKey = profileApps.associateBy { it.profileScopedKey }

		val items = ArrayList<DashItem>()
		val folderedKeys = HashSet<String>()
		if (state != null) {
			for (folder in state.folders) {
				val apps = folder.appKeys.mapNotNull { byKey[it] }
				if (apps.size >= 2) {
					items.add(DashItem.FolderItem(folder, apps))
					apps.forEach { folderedKeys.add(it.profileScopedKey) }
				}
			}
		}

		for (app in profileApps) {
			if (app.profileScopedKey !in folderedKeys) {
				items.add(DashItem.AppItem(app))
			}
		}

		return this.ordered(items, AppSortOrder.current(Preferences.getSharedPreferences(this.context)), state)
	}

	private fun ordered(
		items: List<DashItem>,
		sortOrder: AppSortOrder,
		state: ProfileState?,
	): List<DashItem> {
		val stats = AppUsageStats(this.context)

		if (sortOrder == AppSortOrder.CUSTOM && state != null) {
			val index = HashMap<String, Int>()
			state.order.forEachIndexed { i, id -> index[id] = i }
			val tieBreak = DashComparators.forOrder(AppSortOrder.ALPHABETICAL, stats)

			return items.sortedWith(Comparator { a, b ->
				val ia = index[this.itemId(a)] ?: Int.MAX_VALUE
				val ib = index[this.itemId(b)] ?: Int.MAX_VALUE
				if (ia != ib) ia.compareTo(ib) else tieBreak.compare(a, b)
			})
		}

		return items.sortedWith(DashComparators.forOrder(sortOrder, stats))
	}

	// --- Mutations ---------------------------------------------------------

	/**
	 * Creates a folder containing [a] then [b] (both must be loose apps of the
	 * same profile), returning its id, or null if they are the same app.
	 */
	fun createFolder(a: App, b: App): String? {
		if (a == b) {
			return null
		}

		val state = this.states.getOrPut(this.profileKey(a.user)) { ProfileState() }
		this.removeKeyFromFolders(state, a.profileScopedKey)
		this.removeKeyFromFolders(state, b.profileScopedKey)

		val folder = Folder(Folder.newId(), listOf(
			FolderMember.AppMember(a.profileScopedKey),
			FolderMember.AppMember(b.profileScopedKey),
		))
		state.folders.add(folder)

		// Manual order: the folder takes a's slot; b is absorbed.
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

	/**
	 * Adds [app] to the folder [folderId]. Returns false (caller shows a toast)
	 * when the folder is already full ([MAX_FOLDER_APPS]).
	 */
	fun addToFolder(folderId: String, app: App): Boolean {
		val (profileKey, _) = this.locate(folderId) ?: return false
		val state = this.states[profileKey] ?: return false

		val current = state.folders.first { it.id == folderId }
		if (current.appKeys.contains(app.profileScopedKey)) {
			return true
		}
		if (current.appKeys.size >= MAX_FOLDER_APPS) {
			return false
		}

		this.removeKeyFromFolders(state, app.profileScopedKey)

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

	/**
	 * Removes the app from the folder; the app becomes loose again. A folder left
	 * with a single app is dissolved.
	 */
	fun removeFromFolder(folderId: String, appKey: String) {
		val (profileKey, index) = this.locate(folderId) ?: return
		val state = this.states[profileKey] ?: return
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
			// Folder dissolves: its leftover app and the removed app go loose.
			this.spliceFolderOut(state, folderId, keptApps.map { it.key } + appKey)
		} else {
			state.folders[index] = folder.copy(members = kept)
			this.insertAfterFolder(state, folderId, appKey)
		}

		this.save()
	}

	/** Reorders a member within a folder (custom ordering only). */
	fun moveAppInFolder(folderId: String, fromIndex: Int, toIndex: Int) {
		val (profileKey, index) = this.locate(folderId) ?: return
		val state = this.states[profileKey] ?: return
		val folder = state.folders[index]
		val members = folder.members.toMutableList()
		if (fromIndex !in members.indices || toIndex !in members.indices) {
			return
		}

		members.add(toIndex, members.removeAt(fromIndex))
		state.folders[index] = folder.copy(members = members)
		this.save()
	}

	/** Deletes the folder; its members return as loose apps at the folder's slot. */
	fun deleteFolder(folderId: String) {
		val (profileKey, index) = this.locate(folderId) ?: return
		val state = this.states[profileKey] ?: return
		val folder = state.folders.removeAt(index)
		this.spliceFolderOut(state, folderId, folder.appKeys)
		this.save()
	}

	/**
	 * Applies a manual reorder on [profile]'s page: captures the current display
	 * order, moves the item, and stores it as the profile's manual order.
	 */
	fun moveItem(profile: UserHandle?, fromIndex: Int, toIndex: Int) {
		val items = this.dashItems(profile)
		if (fromIndex !in items.indices || toIndex !in items.indices) {
			return
		}

		val ids = items.mapTo(ArrayList()) { this.itemId(it) }
		ids.add(toIndex, ids.removeAt(fromIndex))

		val state = this.states.getOrPut(this.profileKey(profile)) { ProfileState() }
		state.order.clear()
		state.order.addAll(ids)
		this.save()
	}

	// --- Helpers -----------------------------------------------------------

	private fun locate(folderId: String): Pair<String, Int>? {
		for ((profileKey, state) in this.states) {
			val index = state.folders.indexOfFirst { it.id == folderId }
			if (index >= 0) {
				return profileKey to index
			}
		}

		return null
	}

	/** Strips [key] from every folder of [state], dissolving any that drop to ≤1 app. */
	private fun removeKeyFromFolders(state: ProfileState, key: String) {
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
					this.spliceFolderOut(state, folder.id, keptApps.map { it.key })
					continue
				} else {
					state.folders[i] = folder.copy(members = kept)
				}
			}
			i++
		}
	}

	/** Replaces the folder's order slot with the given loose app ids. */
	private fun spliceFolderOut(state: ProfileState, folderId: String, appKeys: List<String>) {
		val appIds = appKeys.map { this.appItemId(it) }
		appIds.forEach { state.order.remove(it) }

		val pos = state.order.indexOf(this.folderItemId(folderId))
		if (pos >= 0) {
			state.order.removeAt(pos)
			state.order.addAll(pos, appIds)
		} else {
			state.order.addAll(appIds)
		}
	}

	private fun insertAfterFolder(state: ProfileState, folderId: String, appKey: String) {
		val appId = this.appItemId(appKey)
		state.order.remove(appId)
		val pos = state.order.indexOf(this.folderItemId(folderId))
		if (pos >= 0) {
			state.order.add(pos + 1, appId)
		} else {
			state.order.add(appId)
		}
	}

	private fun profileKey(user: UserHandle?): String =
		if (user == null) PERSONAL else Profiles.serialOf(this.context, user).toString()

	private fun appItemId(key: String) = APP_PREFIX + key
	private fun folderItemId(id: String) = FOLDER_PREFIX + id

	private fun itemId(item: DashItem): String = when (item) {
		is DashItem.AppItem -> this.appItemId(item.app.profileScopedKey)
		is DashItem.FolderItem -> this.folderItemId(item.folder.id)
	}

	companion object {
		/** Max apps per dash folder (a 3x3 contents grid). */
		const val MAX_FOLDER_APPS = FolderGrid.MAX_CELLS

		private const val PERSONAL = "personal"
		private const val APP_PREFIX = "app:"
		private const val FOLDER_PREFIX = "folder:"
	}
}
