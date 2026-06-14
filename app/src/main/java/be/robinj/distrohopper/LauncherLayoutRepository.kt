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
 * The launcher's folders, layered over the pinned apps held by [AppRepository] —
 * the launcher counterpart to [DashLayoutRepository], partitioned per **desktop**.
 * Produces the [LauncherItem] list the pinned bar renders (loose pinned apps +
 * folders).
 *
 * Unlike the dash, the launcher has no separate order: the bar's order is the
 * pinned order ([AppRepository.pinnedOn]), and a folder simply renders at its
 * first member's position. This layer therefore stores only folder **membership**
 * (which pinned apps are grouped, and in what in-folder order). Reordering the
 * bar reorders the pinned model itself ([AppRepository.reorderPinned]); a
 * missing/unreadable layout degrades to every pinned app rendering loose.
 * Deleting a launcher folder **unpins its members** (the spec: the folder and
 * its members leave the launcher).
 *
 * Not internally synchronised: mutations/reads happen on the UI thread.
 */
class LauncherLayoutRepository(
	private val context: Context,
	private val appRepository: AppRepository,
) {
	private val foldersByDesktop = HashMap<Int, ArrayList<Folder>>()

	private val _revision = MutableStateFlow(0L)
	val revision: StateFlow<Long> = this._revision.asStateFlow()

	// --- Loading / saving --------------------------------------------------

	fun load() {
		this.foldersByDesktop.clear()
		for ((desktop, layout) in LauncherLayoutStorage.read(this.prefs())) {
			this.foldersByDesktop[desktop] = ArrayList(layout.folders)
		}

		this.reconcile()
		this.bump()
	}

	/** Drops folder members no longer pinned on their desktop, dissolving any folder left with ≤1 app. */
	fun reconcile() {
		var changed = false

		for ((desktop, folders) in this.foldersByDesktop) {
			val pinnedKeys = this.appRepository.pinnedOn(desktop).mapTo(HashSet()) { it.profileScopedKey }
			var i = 0
			while (i < folders.size) {
				val folder = folders[i]
				val kept = folder.members.filter {
					it !is FolderMember.AppMember || pinnedKeys.contains(it.key)
				}
				val keptApps = kept.filterIsInstance<FolderMember.AppMember>()
				if (keptApps.size <= 1) {
					folders.removeAt(i)
					changed = true
					continue
				} else if (kept.size != folder.members.size) {
					folders[i] = folder.copy(members = kept)
					changed = true
				}
				i++
			}
		}

		if (changed) {
			this.save()
		}
	}

	private fun save() {
		LauncherLayoutStorage.write(this.prefs(), this.foldersByDesktop.mapValues { (_, folders) ->
			LauncherLayoutStorage.DesktopLayout(folders.toList())
		})
		this.bump()
	}

	private fun bump() {
		this._revision.value = this._revision.value + 1
	}

	private fun prefs() = Preferences.getSharedPreferences(this.context, Preferences.LAUNCHER_LAYOUT)

	// --- Reading -----------------------------------------------------------

	/**
	 * The launcher items for [desktop]'s bar: the pinned apps in pinned order,
	 * with folders rendered (once) at their first member's position.
	 */
	fun launcherItems(desktop: Int): List<LauncherItem> {
		val pinnedApps = this.appRepository.pinnedOn(desktop)
		val byKey = pinnedApps.associateBy { it.profileScopedKey }

		val folderByKey = HashMap<String, Folder>()
		val resolved = HashMap<String, List<App>>()
		for (folder in this.foldersByDesktop[desktop].orEmpty()) {
			val apps = folder.appKeys.mapNotNull { byKey[it] }
			if (apps.size >= 2) {
				resolved[folder.id] = apps
				apps.forEach { folderByKey[it.profileScopedKey] = folder }
			}
		}

		val items = ArrayList<LauncherItem>()
		val emitted = HashSet<String>()
		for (app in pinnedApps) {
			val folder = folderByKey[app.profileScopedKey]
			if (folder != null) {
				if (emitted.add(folder.id)) {
					items.add(LauncherItem.LauncherFolder(folder, resolved.getValue(folder.id)))
				}
			} else {
				items.add(LauncherItem.LauncherApp(app))
			}
		}

		return items
	}

	// --- Mutations ---------------------------------------------------------

	/** Creates a folder of [a] then [b] (both pinned on [desktop]); returns its id. */
	fun createFolder(desktop: Int, a: App, b: App): String? {
		if (a == b) {
			return null
		}

		val folders = this.foldersByDesktop.getOrPut(desktop) { ArrayList() }
		this.removeKeyFromFolders(folders, a.profileScopedKey)
		this.removeKeyFromFolders(folders, b.profileScopedKey)

		val folder = Folder(Folder.newId(), listOf(
			FolderMember.AppMember(a.profileScopedKey),
			FolderMember.AppMember(b.profileScopedKey),
		))
		folders.add(folder)

		this.save()
		return folder.id
	}

	/** Adds [app] to [folderId]; false (caller toasts) when the folder is full. */
	fun addToFolder(folderId: String, app: App): Boolean {
		val (desktop, _) = this.locate(folderId) ?: return false
		val folders = this.foldersByDesktop[desktop] ?: return false

		val current = folders.first { it.id == folderId }
		if (current.appKeys.contains(app.profileScopedKey)) {
			return true
		}
		if (current.appKeys.size >= MAX_FOLDER_APPS) {
			return false
		}

		this.removeKeyFromFolders(folders, app.profileScopedKey)
		val index = folders.indexOfFirst { it.id == folderId }
		if (index < 0) {
			return false
		}
		folders[index] = folders[index].copy(
			members = folders[index].members + FolderMember.AppMember(app.profileScopedKey))

		this.save()
		return true
	}

	/** Removes the app from the folder; it stays pinned, loose again. Dissolves at ≤1. */
	fun removeFromFolder(folderId: String, appKey: String) {
		val (desktop, index) = this.locate(folderId) ?: return
		val folders = this.foldersByDesktop[desktop] ?: return
		val folder = folders[index]
		if (!folder.appKeys.contains(appKey)) {
			return
		}

		val kept = folder.members.filterNot { it is FolderMember.AppMember && it.key == appKey }
		if (kept.filterIsInstance<FolderMember.AppMember>().size <= 1) {
			folders.removeAt(index)
		} else {
			folders[index] = folder.copy(members = kept)
		}

		this.save()
	}

	/** Reorders a member within a folder. */
	fun moveAppInFolder(folderId: String, fromIndex: Int, toIndex: Int) {
		val (desktop, index) = this.locate(folderId) ?: return
		val folders = this.foldersByDesktop[desktop] ?: return
		val members = folders[index].members.toMutableList()
		if (fromIndex !in members.indices || toIndex !in members.indices) {
			return
		}

		members.add(toIndex, members.removeAt(fromIndex))
		folders[index] = folders[index].copy(members = members)
		this.save()
	}

	/**
	 * Deletes the folder AND unpins its member apps from [desktop] (the launcher
	 * spec: the folder and its members leave the launcher).
	 */
	fun deleteFolder(folderId: String) {
		val (desktop, index) = this.locate(folderId) ?: return
		val folders = this.foldersByDesktop[desktop] ?: return
		val folder = folders.removeAt(index)

		val byKey = this.appRepository.installedAppsMap()
		for (key in folder.appKeys) {
			byKey[key]?.let { this.appRepository.unpin(it, desktop) }
		}
		this.appRepository.savePinnedApps()

		this.save()
	}

	// --- Helpers -----------------------------------------------------------

	private fun locate(folderId: String): Pair<Int, Int>? {
		for ((desktop, folders) in this.foldersByDesktop) {
			val index = folders.indexOfFirst { it.id == folderId }
			if (index >= 0) {
				return desktop to index
			}
		}

		return null
	}

	private fun removeKeyFromFolders(folders: ArrayList<Folder>, key: String) {
		var i = 0
		while (i < folders.size) {
			val folder = folders[i]
			if (folder.appKeys.contains(key)) {
				val kept = folder.members.filterNot { it is FolderMember.AppMember && it.key == key }
				if (kept.filterIsInstance<FolderMember.AppMember>().size <= 1) {
					folders.removeAt(i)
					continue
				} else {
					folders[i] = folder.copy(members = kept)
				}
			}
			i++
		}
	}

	companion object {
		/** Max apps per launcher folder (a 3x3 contents grid). */
		const val MAX_FOLDER_APPS = FolderGrid.MAX_CELLS
	}
}
