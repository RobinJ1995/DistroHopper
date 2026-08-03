package be.robinj.distrohopper.preferences

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.InsetsHelper
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.lens.localfiles.SearchFolderStore

/**
 * Manages the folders the Local files lens searches. Reached from that lens's
 * row in [LensPreferencesActivity].
 *
 * Adding a folder goes through the system directory picker, so this screen
 * never asks for a permission — the grant it comes back with is the permission.
 */
class LocalFilesFoldersActivity : AppCompatActivity() {
	private lateinit var store: SearchFolderStore
	private val folders = ArrayList<Uri>()
	private lateinit var adapter: FolderAdapter

	private val pickFolder = this.registerForActivityResult(
		ActivityResultContracts.OpenDocumentTree()
	) { treeUri ->
		if (treeUri == null) {
			return@registerForActivityResult // The user backed out of the picker //
		}

		try {
			this.store.add(treeUri)
			this.refresh()
		} catch (ex: Exception) {
			ExceptionHandler(ex).show(this)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		try {
			this.setContentView(R.layout.activity_local_files_folders)
			InsetsHelper.applySystemBarsPadding(this)

			this.store = SearchFolderStore(this)
			this.adapter = FolderAdapter()

			val list = this.findViewById<ListView>(R.id.lvFolders)
			list.adapter = this.adapter
			list.emptyView = this.findViewById<TextView>(R.id.tvEmpty)

			this.findViewById<Button>(R.id.btnAddFolder)
				.setOnClickListener { this.pickFolder.launch(null) }

			this.refresh()
		} catch (ex: Exception) {
			ExceptionHandler(ex).show(this)
		}
	}

	override fun onResume() {
		super.onResume()

		// A folder's grant can disappear while we're away (storage unmounted,
		// folder deleted), and the store prunes those as it reads //
		this.refresh()
	}

	private fun refresh() {
		this.folders.clear()
		this.folders.addAll(this.store.folders())
		this.adapter.notifyDataSetChanged()
	}

	private inner class FolderAdapter : ArrayAdapter<Uri>(
		this, R.layout.widget_local_files_folder_list_item,
		this@LocalFilesFoldersActivity.folders,
	) {
		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val view = convertView ?: LayoutInflater.from(this.context)
				.inflate(R.layout.widget_local_files_folder_list_item, parent, false)
			val treeUri = this.getItem(position)!!

			view.findViewById<TextView>(R.id.tvFolderName).text =
				this@LocalFilesFoldersActivity.store.displayName(treeUri)

			view.findViewById<ImageView>(R.id.ivRemoveFolder).setOnClickListener {
				try {
					this@LocalFilesFoldersActivity.store.remove(treeUri)
					this@LocalFilesFoldersActivity.refresh()
				} catch (ex: Exception) {
					ExceptionHandler(ex).show(this@LocalFilesFoldersActivity)
				}
			}

			return view
		}
	}
}
